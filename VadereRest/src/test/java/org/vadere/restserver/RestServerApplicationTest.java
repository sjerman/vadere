package org.vadere.restserver;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.vadere.util.test.TestResourceHandler;

@SpringBootTest
class RestServerApplicationTest implements TestResourceHandler {

  @Autowired SimulationController controller;

  @Test
  void contextLoads() throws SimulationException {
    String json = getTestFileAsString("bus_station.scenario");
    String id = controller.start(json);
    controller.waitUntilComplete(id);
    SimulationStatusDto status = controller.status(id);
    System.out.println("Status:" + status);
    System.out.println("Result" + controller.result(id));
    // controller.getZip(id);
    controller.cleanup(id);
  }

  @Test
  void contextLoads2() throws SimulationException, InterruptedException {
    String json = getTestFileAsString("bus_station.scenario");
    String id = controller.start(json);
    Thread.sleep(10_000);
    SimulationStatusDto status = controller.status(id);
    System.out.println("Status: " + status);
    controller.waitUntilComplete(id);
    status = controller.status(id);
    System.out.println("Status: " + status);
    System.out.println("Result: " + controller.result(id));
    controller.cleanup(id);
  }

  @Override
  public Path getTestDir() {
    return getPathFromResources("/test-file");
  }
}
