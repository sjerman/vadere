package org.vadere.restserver;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vadere.restserver.SimulationStatusDto.Person;
import org.vadere.restserver.SimulationStatusDto.Status;
import org.vadere.simulator.control.simulation.PassiveCallback;
import org.vadere.simulator.control.simulation.RemoteRunListener;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.projects.RunnableFinishedListener;

public class ResultListener
    implements RunnableFinishedListener, RemoteRunListener, PassiveCallback {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResultListener.class);
  Domain scenario;

  private SimulationStatusDto status = new SimulationStatusDto();

  @Override
  public void finished(Runnable runnable) {
    LOGGER.info("Finished");
    synchronized (status) {
      status.getPeople().clear();
      status.setStatus(Status.FINISHED);
    }
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
    synchronized (status) {
      status.getPeople().clear();
      status.setStatus(Status.FINISHED);
    }
  }

  @Override
  public void preLoop(double simTimeInSec) {}

  @Override
  public void postLoop(double simTimeInSec) {}

  @Override
  public void preUpdate(double simTimeInSec) {}

  @Override
  public void postUpdate(double simTimeInSec) {
    int count = scenario.getTopography().getPedestrianDynamicElements().getElements().size();
    LOGGER.trace("Passive posUpdate @ {}, {} pedestrians", Math.rint(simTimeInSec), count);
    synchronized (status) {
      status.setStatus(Status.RUNNING);
      List<Person> people = status.getPeople();
      people.clear();
      scenario
          .getTopography()
          .getPedestrianDynamicElements()
          .getElements()
          .stream()
          .forEach(
              p -> {
                people.add(
                    new Person(
                        p.getSource().getId(),
                        p.getNextTargetId(),
                        p.getPosition().x,
                        p.getPosition().y));
              });
    }
  }

  @Override
  public void setDomain(Domain scenario) {
    this.scenario = scenario;
  }

  public SimulationStatusDto getStatus() {

    synchronized (status) {
      return new SimulationStatusDto(status);
    }
  }
}
