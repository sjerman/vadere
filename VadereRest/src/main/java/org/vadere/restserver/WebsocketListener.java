package org.vadere.restserver;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vadere.simulator.control.simulation.PassiveCallback;
import org.vadere.simulator.control.simulation.RemoteRunListener;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.projects.RunnableFinishedListener;
import org.vadere.util.geometry.shapes.VPoint;

public class WebsocketListener implements RunnableFinishedListener, RemoteRunListener, PassiveCallback{

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketListener.class);
    Domain scenario;

    @Override
    public void finished(Runnable runnable) {
        LOGGER.info("Finished");
    }
    @Override
    public void notifySimStepListener() {
        LOGGER.info("Sim Step");
    }
    @Override
    public void notifySimulationEndListener() {
        LOGGER.info("Sim end");
    }
    @Override
    public void simulationStoppedEarlyListener(double time) {
        LOGGER.info("Finished Early");
    }
    @Override
    public void preLoop(double simTimeInSec) {
    }
    @Override
    public void postLoop(double simTimeInSec) {
    }
    @Override
    public void preUpdate(double simTimeInSec) {
    }
    @Override
    public void postUpdate(double simTimeInSec) {
        int count = scenario.getTopography().getPedestrianDynamicElements().getElements().size();
        LOGGER.info("Passive posUpdate @ {}, {} pedestrians", Math.rint(simTimeInSec), count);
        List<VPoint> points = scenario.getTopography().getPedestrianDynamicElements().getElements().stream().map(p -> p.getPosition()).collect(Collectors.toList());
        LOGGER.info("points: {}", Strings.join(points, ' '));
    }
    @Override
    public void setDomain(Domain scenario) {
    this.scenario = scenario;
  }

}
