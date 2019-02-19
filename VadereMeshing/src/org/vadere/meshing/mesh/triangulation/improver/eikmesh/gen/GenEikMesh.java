package org.vadere.meshing.mesh.triangulation.improver.eikmesh.gen;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.meshing.mesh.IllegalMeshException;
import org.vadere.meshing.mesh.inter.IFace;
import org.vadere.meshing.mesh.inter.IHalfEdge;
import org.vadere.meshing.mesh.inter.IMesh;
import org.vadere.meshing.mesh.inter.IMeshSupplier;
import org.vadere.meshing.mesh.inter.IIncrementalTriangulation;
import org.vadere.meshing.mesh.inter.IVertex;
import org.vadere.meshing.mesh.triangulation.improver.IMeshImprover;
import org.vadere.meshing.mesh.triangulation.improver.distmesh.Parameters;
import org.vadere.meshing.mesh.triangulation.improver.eikmesh.EikMeshPoint;
import org.vadere.meshing.mesh.triangulation.triangulator.inter.ITriangulator;
import org.vadere.meshing.mesh.triangulation.triangulator.gen.GenUniformRefinementTriangulatorSFC;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.math.IDistanceFunction;
import org.vadere.util.geometry.shapes.IPoint;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.geometry.shapes.VTriangle;
import org.vadere.util.math.DistanceFunction;
import org.vadere.meshing.mesh.triangulation.IEdgeLengthFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Benedikt Zoennchen
 */
public class GenEikMesh<P extends EikMeshPoint, CE, CF, V extends IVertex<P>, E extends IHalfEdge<CE>, F extends IFace<CF>> implements IMeshImprover<P, CE, CF, V, E, F>, ITriangulator<P, CE, CF, V, E, F> {

	private boolean illegalMovement = false;

	private GenUniformRefinementTriangulatorSFC triangulatorSFC;
	private IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation;

	private IDistanceFunction distanceFunc;
	private IEdgeLengthFunction edgeLengthFunc;
	private VRectangle bound;
	private double scalingFactor;
	private double deps;
	private static final int MAX_STEPS = 200;
	private int nSteps;
	private double initialEdgeLen;

	private boolean runParallel = false;
	private boolean profiling = false;
	private double minDeltaTravelDistance = 0.0;
	private double delta = Parameters.DELTAT;
	private Collection<? extends VShape> shapes;
	private Map<V, VPoint> fixPointRelation;
	private final IMeshSupplier<P, CE, CF, V, E, F> meshSupplier;
	private boolean meshMode;

	// only for logging
    private static final Logger log = LogManager.getLogger(GenEikMesh.class);

	public GenEikMesh(
			@NotNull final IDistanceFunction distanceFunc,
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen,
			@NotNull final IIncrementalTriangulation<P, CE, CF, V, E, F> triangulation) {
		this.shapes = new ArrayList<>();
		this.bound = null;
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.initialEdgeLen =initialEdgeLen;
		this.deps = 0.0001 * initialEdgeLen;
		this.nSteps = 0;
		this.meshSupplier = null;
		this.fixPointRelation = new HashMap<>();
		this.triangulatorSFC = null;
		this.triangulation = triangulation;
		this.meshMode = true;
	}

	public GenEikMesh(
            @NotNull final IDistanceFunction distanceFunc,
            @NotNull final IEdgeLengthFunction edgeLengthFunc,
            final double initialEdgeLen,
            @NotNull final VRectangle bound,
            @NotNull final Collection<? extends VShape> shapes,
            @NotNull final IMeshSupplier<P, CE, CF, V, E, F> meshSupplier) {
		this.shapes = shapes;
		this.bound = bound;
		this.distanceFunc = distanceFunc;
		this.edgeLengthFunc = edgeLengthFunc;
		this.initialEdgeLen =initialEdgeLen;
		this.deps = 0.0001 * initialEdgeLen;
		this.nSteps = 0;
		this.meshSupplier = meshSupplier;
		this.fixPointRelation = new HashMap<>();
		this.meshMode = false;
		this.triangulatorSFC = new GenUniformRefinementTriangulatorSFC(
				meshSupplier,
				bound,
				shapes,
				edgeLengthFunc,
				initialEdgeLen,
				distanceFunc);
		this.triangulatorSFC.init();
	}

	public GenEikMesh(
			@NotNull final IDistanceFunction distanceFunc,
			@NotNull final IEdgeLengthFunction edgeLengthFunc,
			final double initialEdgeLen,
			@NotNull final VRectangle bound,
			@NotNull final IMeshSupplier<P, CE, CF, V, E, F> meshSupplier) {
		this(distanceFunc, edgeLengthFunc, initialEdgeLen, bound, Collections.EMPTY_LIST, meshSupplier);
	}

	public GenEikMesh(@NotNull final VPolygon boundary,
	                  final double initialEdgeLen,
	                  @NotNull final Collection<? extends VShape> shapes,
	                  @NotNull final IMeshSupplier<P, CE, CF, V, E, F> meshSupplier){
		this(new DistanceFunction(boundary, shapes), p -> 1.0, initialEdgeLen, GeometryUtils.bound(boundary.getPoints(), initialEdgeLen), shapes, meshSupplier);
	}

	public void step() {
		improve();
	}

	public void initialize() {
		while (!triangulatorSFC.isFinished()) {
			stepInitialize();
		}
		computeAnchorPointRelation();
	}

	public void stepInitialize() {
		triangulatorSFC.step();
	}

	public boolean initializationFinished() {
		return triangulatorSFC == null || triangulatorSFC.isFinished();
	}

	@Override
	public IIncrementalTriangulation<P, CE, CF, V, E, F> generate() {

		if(!initializationFinished()) {
			initialize();
		}

		double quality = getQuality();
		//log.info("quality: " + quality);
		while (quality < Parameters.qualityMeasurement && nSteps < MAX_STEPS) {
			improve();
			quality = getQuality();
			//log.info("quality: " + quality);
		}

		try {
			removeFacesOutside(distanceFunc);
		} catch (IllegalMeshException e) {
			log.error("error!");
		}
		//removeTrianglesInsideHoles();
		//removeTrianglesOutsideBBox();
		getMesh().garbageCollection();
		return getTriangulation();
	}

	public boolean isFinished() {
		return /*getQuality() >= Parameters.qualityMeasurement || */ nSteps >= MAX_STEPS;
	}


	@Override
	public IMesh<P, CE, CF, V, E, F> getMesh() {
		return getTriangulation().getMesh();
	}

	@Override
    public void improve() {
		synchronized (getMesh()) {

			if(!initializationFinished()) {
				stepInitialize();
			}
			else {
				if(!meshMode) {
					removeFacesAtBoundary();
					getTriangulation().smoothBoundary(distanceFunc);
				}

				if(getTriangulation().isValid()) {
					flipEdges();
					//removeTrianglesInsideHoles();
					//removeTrianglesOutsideBBox();
				}
				else {
					retriangulate();
				}

				scalingFactor = computeEdgeScalingFactor(edgeLengthFunc);
				computeVertexForces();

				updateEdges();
				updateVertices();

				log.info("quality = " + getQuality());
				nSteps++;
			}
		}
    }

    private void retriangulate() {
	    log.info("EikMesh re-triangulates in step " + nSteps);
	    getTriangulation().recompute();
	    //removeTrianglesOutsideBBox();
	    //removeTrianglesInsideObstacles();
	    try {
		    removeFacesOutside(distanceFunc);
	    } catch (IllegalMeshException e) {
		    log.error("error!");
	    }
    }

    @Override
    public IIncrementalTriangulation<P, CE, CF, V, E, F> getTriangulation() {
		return triangulatorSFC == null ? triangulation : triangulatorSFC.getTriangulation();
    }

    @Override
    public synchronized Collection<VTriangle> getTriangles() {
        return getTriangulation().streamTriangles().collect(Collectors.toList());
    }

    /**
     * computes the edge forces / velocities for all half-edge i.e. for each edge twice!
     */
    private void computeForces() {
	    streamEdges().forEach(e -> computeForces(e));
	}

	private void computeVertexForces() {
    	streamVertices().forEach(v -> computeForce(v));
	}

	/**
	 * Computes and sets the overall force acting on a vertex. If the vertex is an anchor vertex
	 * the force will be different: It will act towards the anchor point.
	 *
	 * @param vertex the vertex of interest
	 */
	private void computeForce(final V vertex) {
		EikMeshPoint p1 = getMesh().getPoint(vertex);

		if(fixPointRelation.containsKey(vertex)) {
			VPoint p2 = fixPointRelation.get(vertex);
			VPoint force = new VPoint((p2.getX() - p1.getX()), (p2.getY() - p1.getY())).scalarMultiply(1.0);
			p1.increaseVelocity(force);
			p1.increaseAbsoluteForce(force.distanceToOrigin());
		}
		else {
			for(V v2 : getMesh().getAdjacentVertexIt(vertex)) {
				EikMeshPoint p2 = getMesh().getPoint(v2);

				double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
				double desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5)) * Parameters.FSCALE * scalingFactor;

				//double lenDiff = Math.max(desiredLen - len, 0);
				double lenDiff = desiredLen - len;

				if(lenDiff < 0 /*&& lenDiff > -desiredLen*/) {
					lenDiff *= 0.1;
				}

				VPoint force = new VPoint((p1.getX() - p2.getX()) * (lenDiff / len), (p1.getY() - p2.getY()) * (lenDiff / len));
				p1.increaseVelocity(force);
				p1.increaseAbsoluteForce(force.distanceToOrigin());
			}
		}
	}

    /**
     * Computes the edge force / velocity for a single half-edge and adds it to its end vertex.
     *
     * @param edge
     */
    private void computeForces(final E edge) {
        EikMeshPoint p1 = getMesh().getPoint(edge);
        EikMeshPoint p2 = getMesh().getPoint(getMesh().getPrev(edge));

        double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
        double desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5)) * Parameters.FSCALE * scalingFactor;

		double lenDiff = Math.max(desiredLen - len, 0);
        p1.increaseVelocity(new VPoint((p1.getX() - p2.getX()) * (lenDiff / len), (p1.getY() - p2.getY()) * (lenDiff / len)));
    }

	private void computeForcesBossan(final E edge) {
		EikMeshPoint p1 = getMesh().getPoint(edge);
		EikMeshPoint p2 = getMesh().getPoint(getMesh().getPrev(edge));

		double len = Math.sqrt((p1.getX() - p2.getX()) * (p1.getX() - p2.getX()) + (p1.getY() - p2.getY()) * (p1.getY() - p2.getY()));
		double desiredLen = edgeLengthFunc.apply(new VPoint((p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5)) * Parameters.FSCALE * scalingFactor;

		double lenDiff = Math.max(desiredLen - len, 0);
		p1.increaseVelocity(new VPoint((p1.getX() - p2.getX()) * (lenDiff / (len / desiredLen)), (p1.getY() - p2.getY()) * (lenDiff / (len / desiredLen))));
	}
    /**
     * Moves (which may include a back projection) each vertex according to their forces / velocity
     * and resets their forces / velocities. A vertex might be broken (removed by edge collapse)
     * if the forces acting on an boundary vertex are to strong.
     */
    private void updateVertices() {
	    streamVertices().forEach(v -> updateVertex(v));
	}

	/**
	 * Updates a vertex which is not a fix point, that is, the computed force is applied and the vertex move
	 * according to this force. Additionally, the vertex might get back projected if it is outside or it might
	 * break if the forces / the pressure are too large.
	 *
	 * @param vertex the vertex
	 */
	private void updateVertex(final V vertex) {
	    // modify point placement only if it is not a fix point
	    P point = getMesh().getPoint(vertex);
    	if(!isFixedVertex(vertex)) {
		    /*
		     * (1) break / remove the vertex if the forces are to large / there is to much pressure
		     */
		    if(canBreak(vertex) && isBreaking(vertex)) {
			    // TODO: if the algorithm runs in parallel this might lead to unexpected results! synchronized required!
			    getTriangulation().collapse3DVertex(vertex, true);
		    }
		    /*
		     * (2) otherwise displace the vertex
		     */
		    else {
			    VPoint oldPosition = new VPoint(vertex.getX(), vertex.getY());

			    // (2.1) p_{k+1} = p_k + dt * F(p_k)
			    applyForce(vertex);
			    VPoint forceDisplacement = new VPoint(vertex.getX(), vertex.getY());

			    // (2.2) back projtion
			    VPoint projection = computeProjection(vertex);
			    point.set(projection.getX(), projection.getY());
		    }
		}
	    point.setVelocity(new VPoint(0,0));
	    point.setAbsoluteForce(0);
	}

	/**
	 * Updates all boundary edges. Some of those edges might get split.
	 */
	private void updateEdges() {
		if(!meshMode) {
			getMesh().getBoundaryEdges().forEach(e -> updateEdge(e));
		}
		else {
			getMesh().getEdges().forEach(e -> updateEdge(e));
		}
	}

	/**
	 * Splits an edge if necessary.
	 *
	 * @param edge the edge
	 */
	private void updateEdge(@NotNull final E edge) {
		if(canBreak(edge) && isBreaking(edge)) {
			if(meshMode && getMesh().isBoundary(edge)) {
				getTriangulation().splitEdge(edge, true, p -> p.setFixPoint(true));
			}
			else {
				getTriangulation().splitEdge(edge, true);
			}
		}
	}

	/**
	 * Returns true if the edge can be split, that is the edge can be replaced
	 * by two new edges by splitting the edge and its face into two. The edge
	 * has to be at the boundary.
	 *
	 * @param edge the edge
	 * @return true if the edge can be collapsed / break.
	 */
	private boolean canBreak(@NotNull final E edge) {
		if(!meshMode) {
			return !getMesh().isDestroyed(edge) && getMesh().isAtBoundary(edge);
		} else {
			return !getMesh().isDestroyed(edge) && (getMesh().isAtBoundary(edge)
					|| getMesh().isAtBoundary(getMesh().getNext(edge))
					|| getMesh().isAtBoundary(getMesh().getPrev(edge)));
		}
	}

	/**
	 * Returns true if the edge should be split. That is the case if the edge is long with
	 * respect to the other two edges of the face, i.e. it has to be the longest edge and the
	 * quality of the face is low. The analogy is that the edge breaks because it can not
	 * become any longer.
	 *
	 * @param edge the edge which is tested
	 * @return true if the edge breaks under the pressure of the forces, otherwise false.
	 */
	private boolean isBreaking(@NotNull final E edge) {
		if(!meshMode) {
			return getMesh().isLongestEdge(edge) && faceToQuality(getMesh().getTwinFace(edge)) < Parameters.MIN_SPLIT_TRIANGLE_QUALITY;
		}
		else {
			return (getMesh().isLongestEdge(edge) && getMesh().isBoundary(edge) && faceToQuality(getMesh().getTwinFace(edge)) < Parameters.MIN_SPLIT_TRIANGLE_QUALITY)
					|| isDoubleLongEdge(edge);
		}
	}

	private boolean isDoubleLongEdge(@NotNull final E edge) {

		if(!getMesh().isAtBoundary(edge)) {
			VLine line = getMesh().toLine(edge);
			double factor = 1.5;
			VLine line1 = getMesh().toLine(getMesh().getNext(edge));
			VLine line2 = getMesh().toLine(getMesh().getPrev(edge));

			return getMesh().isAtBoundary(getMesh().getNext(edge)) && line1.length() * factor <= line.length()
					|| getMesh().isAtBoundary(getMesh().getPrev(edge)) && line2.length() * factor <= line.length();
		}
		return false;
	}

	/*private boolean isDoubleLongEdge(@NotNull final E edge) {
		VLine line = getMesh().toLine(edge);
		double factor = 2.5;
		if(getMesh().isAtBoundary(edge)) {
			VLine line1 = getMesh().toLine(getMesh().getNext(edge));
			VLine line2 = getMesh().toLine(getMesh().getPrev(edge));
			return line.length() >= line1.length() * factor || line.length() >= line2.length() * factor;
		}
		else {
			VLine line1 = getMesh().toLine(getMesh().getNext(getMesh().getTwin(edge)));
			VLine line2 = getMesh().toLine(getMesh().getPrev(getMesh().getTwin(edge)));
			VLine line3 = getMesh().toLine(getMesh().getPrev(edge));
			VLine line4 = getMesh().toLine(getMesh().getPrev(edge));
			return line.length() >= line1.length() * factor || line.length() >= line2.length() * factor
					|| line.length() >= line3.length() * factor || line.length() >= line4.length() * factor;
		}
	}*/

	/**
	 * Returns true if the vertex can be collapsed, that is the vertex can be removed
	 * by removing one edge and collapsing the other two. The vertex has to be at
	 * the boundary and has to have degree equal to three. We say that this vertex
	 * can break under the pressure of the forces.
	 *
	 * @param vertex the vertex
	 * @return true if the vertex can be collapsed / break.
	 */
	private boolean canBreak(@NotNull final V vertex) {
    	return getMesh().isAtBoundary(vertex) && getMesh().degree(vertex) == 3;
	}

	/**
	 * Returns true if the vertex should be collapsed. That is the case if the resulting force
	 * acting on the vertex is low but the sum of all absolute partial forces is high. An analogy
	 * might be that the vertex breaks under the pressure of the forces.
	 *
	 * @param vertex the vertex which is tested.
	 * @return true if the vertex breaks under the pressure of the forces, otherwise false.
	 */
	private boolean isBreaking(@NotNull final V vertex) {
		double force = getForce(vertex).distanceToOrigin();
		P point = getMesh().getPoint(vertex);
		return point.getAbsoluteForce() > 0 && force / point.getAbsoluteForce() < Parameters.MIN_FORCE_RATIO;
	}


    /**
     * Computes the projection of Projects vertices. The projection acts towards the boundary of the boundary.
     * EikMesh projects only boundary vertices. Furthermore, to improve the convergence rate EikMesh additionally
     * projects vertices which are inside if the projection is inside a valid circle segment.
     *
     * @param vertex the vertex might be projected
     */
    private VPoint computeProjection(@NotNull final V vertex) {
    	// we only project boundary vertices back
	    if(getMesh().isAtBoundary(vertex)) {

		    P position = getMesh().getPoint(vertex);
		    double distance = distanceFunc.apply(position);

		    double x = position.getX();
		    double y = position.getY();

		    // the gradient (dx, dy)
		    double dGradPX = (distanceFunc.apply(position.toVPoint().add(new VPoint(deps, 0))) - distance) / deps;
		    double dGradPY = (distanceFunc.apply(position.toVPoint().add(new VPoint(0, deps))) - distance) / deps;

		    double projX = dGradPX * distance;
		    double projY = dGradPY * distance;

		    double newX = x - projX;
		    double newY = y - projY;

	    	// back projection towards the inside if the point is outside
	    	if(isOutside(position, distanceFunc)) {
			    return new VPoint(newX, newY);
		    }
		    // back projection towards the inside if the point is inside (to improve the convergence rate of the algorithm)
		    else if(isInsideProjectionValid(vertex, newX, newY)) {
			    return new VPoint(newX, newY);
		    }
	    }

	    return new VPoint(vertex.getX(), vertex.getY());
    }

	/**
	 * Tests if a point is outside which is determined by the <tt>distanceFunc</tt>.
	 *
	 * @param point         the point of interest
	 * @param distanceFunc  the distance function which defines inside and outside
	 *
	 * @return true if the point is outside, false otherwise
	 */
	private boolean isOutside(@NotNull final IPoint point, @NotNull final IDistanceFunction distanceFunc) {
		return distanceFunc.apply(point) > 0;
    }

	/**
	 * Tests if the inside projection is valid which is the case if the angle at the vertex (at the boundary)
	 * is greater than 180 degree or the result of the projection lies inside the segment spanned by the
	 * vertex and its two neighbouring border vertices.
	 *
	 * @param vertex    the vertex
	 * @param newX      x-coordinate of the new position (after projection)
	 * @param newY      y-coordinate of the new position (after projection)
	 *
	 * @return true if the inside projection is valid, false otherwise
	 */
	private boolean isInsideProjectionValid(@NotNull final V vertex, final double newX, final double newY) {
	    Optional<E> boundaryEdgeOpt = getMesh().getBoundaryEdge(vertex);

	    if(!boundaryEdgeOpt.isPresent()) {
	    	return false;
	    }
	    else {
	    	// TODO: if the algorithm runs in parallel this might lead to unexpected results!
	    	E boundaryEdge = boundaryEdgeOpt.get();
		    VPoint p = getMesh().toPoint(vertex);
		    VPoint q = getMesh().toPoint(getMesh().getNext(boundaryEdge));
		    VPoint r = getMesh().toPoint(getMesh().getPrev(boundaryEdge));
		    double angle = GeometryUtils.angle(r, p, q);
		    return angle > Math.PI || (GeometryUtils.isLeftOf(r, p, newX, newY) && GeometryUtils.isLeftOf(p, q, newX, newY));
	    }
    }

	/**
	 * Computes the anchor to vertex partial relation. This relation gives some
	 * vertices their anchor point. If a vertex has an anchor point it will be driven
	 * towards this point (instead of the normal movement).
	 */
	private void computeAnchorPointRelation() {
		for(VPoint fixPoint : generateAnchorPoints(shapes)) {
			V closest = null;
			double distance = Double.MAX_VALUE;
			for(V vertex : getMesh().getVertices()) {
				if (closest == null || distance > vertex.distance(fixPoint)) {
					closest = vertex;
					distance = vertex.distance(fixPoint);
				}
			}
			fixPointRelation.put(closest, fixPoint);
		}
	}

    /**
     * Flips all edges which do not fulfill the Delaunay criterion and therefore being illegal.
     * Note that this is not a recursive flipping and therefore the result might not be a
     * Delaunay triangulation. However, due to the nature of the EikMesh algorithm (high quality initial mesh)
     * the triangulation is Delaunay in almost all cases and if not it is almost Delaunay.
     *
     * @return true, if any flip was necessary, false otherwise.
     */
    private boolean flipEdges() {
	    if(runParallel) {
	        streamEdges().filter(e -> getTriangulation().isIllegal(e)).forEach(e -> getTriangulation().flipSync(e));
        }
        else {
		    streamEdges().filter(e -> getTriangulation().isIllegal(e)).forEach(e -> getTriangulation().flip(e));
        }
        return false;
    }

    /**
     * Computation of the factor which transforms relative edge length into absolute ones.
     */
    private double computeEdgeScalingFactor(@NotNull final IEdgeLengthFunction edgeLengthFunc) {
        double edgeLengthSum = streamEdges()
                .map(edge -> getMesh().toLine(edge))
                .mapToDouble(line -> line.length())
                .sum();

        double desiredEdgeLenSum = streamEdges()
                .map(edge -> getMesh().toLine(edge))
                .map(line -> line.midPoint())
                .mapToDouble(midPoint -> edgeLengthFunc.apply(midPoint)).sum();
        return Math.sqrt((edgeLengthSum * edgeLengthSum) / (desiredEdgeLenSum * desiredEdgeLenSum));
    }


    // helper methods
    private Stream<E> streamEdges() {
        return runParallel ? getMesh().streamEdgesParallel() : getMesh().streamEdges();
    }

    private Stream<V> streamVertices() {
        return runParallel ? getMesh().streamVerticesParallel() : getMesh().streamVertices();
    }

	/**
	 * Returns true if and only if the vertex {@link V} is a fix point.
	 *
	 * @param vertex the vertex of interest
	 * @return true if and only if the vertex {@link V} is a fix point.
	 */
	private boolean isFixedVertex(final V vertex) {
		return getMesh().getPoint(vertex).isFixPoint() || meshMode && getMesh().isAtBoundary(vertex);
	}

	/**
	 * Returns the force which is currently i.e. which was computed by
	 * {@link GenEikMesh#computeForce(IVertex)} applied to the vertex.
	 *
	 * @param vertex the vertex of interest
	 * @return the force which is currently applied to the vertex
	 */
	private IPoint getForce(final V vertex) {
		return getMesh().getPoint(vertex).getVelocity();
	}

	/**
	 * Applies the force of the vertex to the vertex which results
	 * in an displacement of the vertex.
	 *
	 * @param vertex the vertex of interest
	 */
	private void applyForce(final V vertex) {
		IPoint velocity = getForce(vertex);
		IPoint movement = velocity.scalarMultiply(delta);
		getMesh().getPoint(vertex).add(movement);
	}

	/**
	 * Computes the set of anchor points. An anchor point replaces fix points in EikMesh.
	 * Instead of inserting fix points EikMesh pushes (via forces) close points of
	 * an anchor point towards this anchor point. For each shape the the points of the
	 * path defining the shape will be added to the set of anchor points.
	 *
	 * @param shapes a list of shapes.
	 * @return the set of anchor points
	 */
	private Set<VPoint> generateAnchorPoints(@NotNull Collection<? extends  VShape> shapes) {
		return shapes.stream()
				.flatMap(shape -> shape.getPath().stream())
				.filter(p -> bound.contains(p))
				.collect(Collectors.toSet());
	}

	/**
	 * Removes all faces neighbouring a boundary which can and should be removed.
	 *
	 * This takes O(n) time where n is the number of removed faces which will be consumed.
	 */
	private void removeFacesAtBoundary() {
		Predicate<F> isSeparated = f -> getMesh().isSeparated(f);
		Predicate<F> isInvalid = f -> !getTriangulation().isValid(f);
		Predicate<F> isOfLowQuality = f -> faceToQuality(f) < Parameters.MIN_TRIANGLE_QUALITY && !isShortBoundaryEdge(f);
		Predicate<F> isBoundary = f -> getMesh().isBoundary(f);

		Predicate<F> mergePredicate = isSeparated.or(isInvalid).or(isOfLowQuality);
		try {
			getTriangulation().removeFacesAtBoundary(mergePredicate, isBoundary);
		} catch (IllegalMeshException e) {
			log.error("error!");
		}
	}

	private boolean isShortBoundaryEdge(@NotNull final F face) {
		E edge = getMesh().getBoundaryEdge(face).get();
		// corner => can be deleted

		VLine l1 = getMesh().toLine(edge);
		VLine l2 = getMesh().toLine(getMesh().getNext(edge));
		VLine l3 = getMesh().toLine(getMesh().getPrev(edge));

		if(l1.length() < l2.length() || l1.length() < l3.length()) {
			return true;
		}

		return false;
	}

	/*private void removeTrianglesInsideHoles() {
		List<F> holes = triangulation.getMesh().getHoles();
		Predicate<F> mergeCondition = f -> !triangulation.getMesh().isBoundary(f) && distanceFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0;
		for(F face : holes) {
			triangulation.mergeFaces(face, mergeCondition, true);
		}
	}

	private void removeTrianglesInsideObstacles() {
		List<F> faces = triangulation.getMesh().getFaces();
		for(F face : faces) {
			if(!triangulation.getMesh().isDestroyed(face) && !triangulation.getMesh().isHole(face)) {
				triangulation.createHole(face, f -> distanceFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
			}
		}
	}

	public void removeTrianglesOutsideBBox() {
		triangulation.shrinkBorder(f -> distanceFunc.apply(triangulation.getMesh().toTriangle(f).midPoint()) > 0, true);
	}*/



	/*private void removeBoundaryLowQualityTriangles() {

		List<F> holes = triangulation.getMesh().getHoles();


		Predicate<F> mergeCondition = f ->
				(!triangulation.getMesh().isDestroyed(f) && !triangulation.getMesh().isBoundary(f) && triangulation.getMesh().isAtBoundary(f)) // at boundary
				&& (!triangulation.isValid(f) || (isCorner(f) || !isShortBoundaryEdge(f)) && faceToQuality(f) < Parameters.MIN_TRIANGLE_QUALITY) // bad quality
		;

		for(F face : holes) {
			List<F> neighbouringFaces = getMesh().streamEdges(face).map(e -> getMesh().getTwinFace(e)).collect(Collectors.toList());
			for (F neighbouringFace : neighbouringFaces) {
				if (mergeCondition.test(neighbouringFace)) {
					triangulation.removeEdges(face, neighbouringFace, true);
				}
			}
		}

		List<F> neighbouringFaces = getMesh().streamEdges(getMesh().getBorder()).map(e -> getMesh().getTwinFace(e)).collect(Collectors.toList());
		for (F neighbouringFace : neighbouringFaces) {
			if (mergeCondition.test(neighbouringFace)) {
				triangulation.removeEdges(getMesh().getBorder(), neighbouringFace, true);
			}
		}

		//triangulation.mergeFaces(getMesh().getBorder(), mergeCondition, true);
	}*/
}
