package org.vadere.restserver;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.vadere.simulator.control.simulation.ScenarioRun;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.SimulationResult;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.simulator.utils.cache.ScenarioCache;

public class SimulationController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResultListener.class);

  @Autowired AsyncTaskExecutor executor;

  Map<String, ResultListener> listeners = new HashMap<>();
  Map<String, Future<?>> executors = new HashMap<>();
  Map<String, ScenarioRun> simulations = new HashMap<>();

  public String start(String json) throws SimulationException {

    String id = UUID.randomUUID().toString();
    try {
      Path output = Files.createTempDirectory("simul").toRealPath();
      Scenario scen = IOVadere.fromJson(json);
      ScenarioCache scenarioCache = ScenarioCache.load(scen, output);
      ResultListener listener = new ResultListener();
      listeners.put(id, listener);
      ScenarioRun run = new ScenarioRun(scen, output.toString(), listener, output, scenarioCache);
      run.addRemoteManagerListener(listener);
      run.addPassiveCallback(listener);
      simulations.put(id, run);
      Future<?> res = executor.submit(run);
      executors.put(id, res);
    } catch (Exception e) {
      listeners.remove(id);
      simulations.remove(id);
      executors.remove(id);
      LOGGER.error("Cannot run simulation", e);
      throw new SimulationException("Cannot start simulation", e);
    }
    return id;
  }

  public SimulationStatusDto status(String id) throws SimulationException {
    if (listeners.containsKey(id)) {
      return listeners.get(id).getStatus();
    }
    throw new SimulationException("Simulation id '" + id + "' does not exist");
  }

  public void waitUntilComplete(String id) throws SimulationException {

    if (executors.containsKey(id)) {
      try {
        executors.get(id).get();
        return;
      } catch (Exception e) {
        throw new SimulationException("Cant get status for '" + id + "'", e);
      }
    }
    throw new SimulationException("Simulation id '" + id + "' does not exist");
  }

  public void getZip(OutputStream os, String id) throws SimulationException {
    if (!simulations.containsKey(id)) {
      throw new SimulationException("Simulation id '" + id + "' does not exist");
    }
    try (ZipOutputStream zos = new ZipOutputStream(os); ) {
      ScenarioRun sim = simulations.get(id);
      Path output = sim.getOutputPath();
      Files.walk(output)
          .filter(Files::isRegularFile)
          .forEach(
              f -> {
                // if (f.)
                try {
                  ZipEntry ze = new ZipEntry(f.getFileName().toString());
                  zos.putNextEntry(ze);
                  Files.copy(f, zos);
                  zos.closeEntry();
                } catch (IOException e) {
                  LOGGER.error("can't access file", e);
                }
              });
    } catch (Exception e) {
      throw new SimulationException("Simulation id '" + id + "' does not exist");
    }
  }

  public void cleanup(String id) throws SimulationException {
    if (!simulations.containsKey(id)) {
      throw new SimulationException("Simulation id '" + id + "' does not exist");
    }
    ScenarioRun sim = simulations.get(id);

    Path output = sim.getOutputPath();

    LOGGER.info("Need to delete {}", output);
    simulations.remove(id);
    listeners.remove(id);
    executors.remove(id);
  }

  public SimulationResult result(String id) throws SimulationException {
    if (!simulations.containsKey(id)) {
      throw new SimulationException("Simulation id '" + id + "' does not exist");
    }
    ScenarioRun sim = simulations.get(id);
    return sim.getSimulationResult();
  }
}
