package org.vadere.simulator.models.sfm;

import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.ode.IntegratorFactory;
import org.vadere.simulator.models.ode.ODEModel;
import org.vadere.simulator.models.potential.FloorGradientProviderFactory;
import org.vadere.simulator.models.potential.PotentialFieldModel;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTargetGrid;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.models.potential.solver.gradients.GradientProvider;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesSFM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.cognition.UnsupportedSelfCategoryException;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.types.GradientProviderType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.parallel.ParallelWorkerUtil;

@ModelClass(isMainModel = true)
public class SocialForceModel extends ODEModel<Pedestrian, AttributesAgent>
    implements PotentialFieldModel {

  private AttributesSFM attributes;
  private GradientProvider floorGradient;
  private Map<Integer, Target> targets;
  private IPotentialFieldTargetGrid potentialFieldTarget;
  private PotentialFieldObstacle potentialFieldObstacle;
  private PotentialFieldAgent potentialFieldPedestrian;
  private List<Model> models = new LinkedList<>();

  @Deprecated
  public SocialForceModel(
      Domain domain,
      AttributesSFM attributes,
      PotentialFieldObstacle potentialFieldObstacle,
      PotentialFieldAgent potentialFieldPedestrian,
      IPotentialFieldTargetGrid potentialFieldTarget,
      AttributesAgent attributesPedestrian,
      Random random) {
    super(
        Pedestrian.class,
        domain,
        IntegratorFactory.createFirstOrderIntegrator(attributes.getAttributesODEIntegrator()),
        new SFMEquations(),
        attributesPedestrian,
        random);
    this.attributes = attributes;
    this.targets = new TreeMap<>();
    this.floorGradient =
        FloorGradientProviderFactory.createFloorGradientProvider(
            attributes.getFloorGradientProviderType(), domain, targets, potentialFieldTarget);

    this.potentialFieldObstacle = potentialFieldObstacle;
    this.potentialFieldPedestrian = potentialFieldPedestrian;
    this.potentialFieldTarget = potentialFieldTarget;
  }

  public SocialForceModel() {
    this.targets = new TreeMap<>();
  }

  @Override
  public void initialize(
      List<Attributes> modelAttributesList,
      Domain domain,
      AttributesAgent attributesPedestrian,
      Random random) {

    this.attributes = Model.findAttributes(modelAttributesList, AttributesSFM.class);

    super.initializeODEModel(
        Pedestrian.class,
        IntegratorFactory.createFirstOrderIntegrator(attributes.getAttributesODEIntegrator()),
        new SFMEquations(),
        attributesPedestrian,
        domain,
        random);

    this.floorGradient =
        FloorGradientProviderFactory.createFloorGradientProvider(
            GradientProviderType.FLOOR_EUCLIDEAN_CONTINUOUS, domain, targets, null);

    IPotentialFieldTargetGrid iPotentialTargetGrid =
        IPotentialFieldTargetGrid.createPotentialField(
            modelAttributesList,
            domain,
            attributesPedestrian,
            attributes.getTargetPotentialModel());

    this.potentialFieldTarget = iPotentialTargetGrid;
    models.add(iPotentialTargetGrid);

    this.potentialFieldObstacle =
        PotentialFieldObstacle.createPotentialField(
            modelAttributesList,
            domain,
            attributesPedestrian,
            random,
            attributes.getObstaclePotentialModel());

    this.potentialFieldPedestrian =
        PotentialFieldAgent.createPotentialField(
            modelAttributesList,
            domain,
            attributesPedestrian,
            random,
            attributes.getPedestrianPotentialModel());

    models.add(this);
  }

  public void rebuildFloorField(final double simTimeInSec) {

    // build list of current targets
    Map<Integer, Target> targets = new HashMap<>();
    for (Pedestrian pedestrian : domain.getTopography().getElements(Pedestrian.class)) {
      if (pedestrian.hasNextTarget()) {
        Target t = domain.getTopography().getTarget(pedestrian.getNextTargetId());
        if (t != null) {
          targets.put(t.getId(), t);
        }
      }
    }

    // if the old targets are equal to the new targets, do not change the
    // floor gradient.
    if (this.targets.equals(targets) && !this.potentialFieldTarget.needsUpdate()) {
      return;
    }

    this.targets = targets;

    this.potentialFieldTarget.update(simTimeInSec);

    floorGradient =
        FloorGradientProviderFactory.createFloorGradientProvider(
            attributes.getFloorGradientProviderType(), domain, targets, this.potentialFieldTarget);
  }

  @Override
  public void preLoop(final double state) {
    super.preLoop(state);
    // setup thread pool if it is not setup already
    int WORKERS_COUNT = 16; // pedestrians.keySet().size();
    ParallelWorkerUtil.setup(WORKERS_COUNT);
  }

  @Override
  public void postLoop(final double simTimeInSec) {
    super.postLoop(simTimeInSec);
    ParallelWorkerUtil.shutdown();
  }

  @Override
  public void update(final double simTimeInSec) {

    rebuildFloorField(simTimeInSec);

    Collection<Pedestrian> pedestrians = domain.getTopography().getElements(Pedestrian.class);

    UnsupportedSelfCategoryException.throwIfPedestriansNotTargetOrientied(
        pedestrians, this.getClass());

    // set gradient provider and pedestrians
    equations.setElements(pedestrians);

    equations.setGradients(
        floorGradient, potentialFieldObstacle, potentialFieldPedestrian, domain.getTopography());

    super.update(simTimeInSec);
  }

  @Override
  public <T extends DynamicElement> Pedestrian createElement(
      VPoint position, int id, Class<T> type) {
    return createElement(position, id, this.elementAttributes, type);
  }

  @Override
  public <T extends DynamicElement> Pedestrian createElement(
      VPoint position, int id, Attributes attr, Class<T> type) {
    AttributesAgent aAttr = (AttributesAgent) attr;

    if (!Pedestrian.class.isAssignableFrom(type))
      throw new IllegalArgumentException("SFM cannot initialize " + type.getCanonicalName());

    AttributesAgent pedAttributes =
        new AttributesAgent(aAttr, registerDynamicElementId(domain.getTopography(), id));
    Pedestrian result = create(position, pedAttributes);
    return result;
  }

  private Pedestrian create(
      @NotNull final VPoint position, @NotNull final AttributesAgent pedAttributes) {
    Pedestrian pedestrian = new Pedestrian(pedAttributes, random);
    pedestrian.setPosition(position);
    return pedestrian;
  }

  @Override
  public VShape getDynamicElementRequiredPlace(@NotNull final VPoint position) {
    return create(position, new AttributesAgent(elementAttributes, -1)).getShape();
  }

  @Override
  public List<Model> getSubmodels() {
    return models;
  }

  @Override
  public IPotentialFieldTarget getPotentialFieldTarget() {
    return potentialFieldTarget;
  }

  @Override
  public PotentialFieldObstacle getPotentialFieldObstacle() {
    return potentialFieldObstacle;
  }

  @Override
  public PotentialFieldAgent getPotentialFieldAgent() {
    return potentialFieldPedestrian;
  }
}
