package org.vadere.restserver;

import java.nio.file.Path;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.vadere.simulator.control.simulation.PassiveCallback;
import org.vadere.simulator.control.simulation.ScenarioRun;
import org.vadere.simulator.projects.Domain;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.IOVadere;
import org.vadere.simulator.utils.cache.ScenarioCache;
import org.vadere.util.test.TestResourceHandler;

@SpringBootTest
class RestServerApplicationTests implements TestResourceHandler {


  @Autowired
  AsyncTaskExecutor executor;

  @Autowired
  WebsocketListener listener;

  @Test
  void contextLoads() {
    String json = getTestFileAsString("bus_station.scenario");

    try {
      Path output = Path.of("output").toRealPath();
     Scenario scen = IOVadere.fromJson(json);
     ScenarioCache scenarioCache = ScenarioCache.load(scen, output);
     ScenarioRun run = new ScenarioRun(scen, listener, output, scenarioCache);
      run.addRemoteManagerListener(listener);
      run.addPassiveCallback(listener);

      Future<?> res = executor.submit(run);
      res.get();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  @Override
  public Path getTestDir() 
  {
    return getPathFromResources("/test-file");
  }


}

