package org.vadere.util.triangulation.adaptive;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.util.geometry.mesh.gen.*;
import org.vadere.util.geometry.mesh.inter.IPointLocator;
import org.vadere.util.geometry.mesh.inter.ITriangulation;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.triangulation.ITriangulationSupplier;
import org.vadere.util.triangulation.triangulator.UniformRefinementTriangulatorCFS;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bzoennchen on 30.01.18.
 */
public class TestUniTriangulation extends JFrame {


    private static final Logger log = LogManager.getLogger(TestUniTriangulation.class);

    private TestUniTriangulation() {

        IDistanceFunction distanceFunc = p -> Math.abs(6 - Math.sqrt(p.getX() * p.getX() + p.getY() * p.getY())) - 4;
        VRectangle bbox = new VRectangle(-11, -11, 22, 22);
        ITriangulationSupplier<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> supplier = () -> ITriangulation.createATriangulation(IPointLocator.Type.DELAUNAY_HIERARCHY, bbox, (x, y) -> new VPoint(x, y));

        UniformRefinementTriangulatorCFS<VPoint, AVertex<VPoint>, AHalfEdge<VPoint>, AFace<VPoint>> uniformRefinementTriangulation =
                new UniformRefinementTriangulatorCFS<>(supplier, bbox, new ArrayList<>(), p -> 5.0, distanceFunc);

        PSMeshingPanel<MeshPoint, PVertex<MeshPoint>, PHalfEdge<MeshPoint>, PFace<MeshPoint>> distmeshPanel =
                new PSMeshingPanel(uniformRefinementTriangulation.init().getMesh(), f -> false, 1000, 800, bbox);
        JFrame frame = distmeshPanel.display();
        frame.setVisible(true);
        frame.setTitle("CPU");



        double avgQuality = 0.0;
        long obscuteTriangles = -1;
        int counter = 0;
        long time = 0;

        StopWatch overAllTime = new StopWatch();
        overAllTime.start();

        while (!uniformRefinementTriangulation.isFinished()) {

            //meshGenerator.improve();
            overAllTime.suspend();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
	        uniformRefinementTriangulation.step();
            distmeshPanel.repaint();
            counter++;
            //System.out.println("Quality: " + meshGenerator.getQuality());
            overAllTime.resume();

            boolean removedSome = true;
        }
        //uniformRefinementTriangulation.finish();

        distmeshPanel.repaint();
        overAllTime.stop();
        log.info("rdy");
    }

    public static void main(String[] args) {
        new TestUniTriangulation();
    }

}
