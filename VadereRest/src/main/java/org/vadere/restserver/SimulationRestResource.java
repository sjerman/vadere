package org.vadere.restserver;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class SimulationRestResource {

  @Autowired SimulationController simulationController;

  @GetMapping(value = "/ping", produces = MediaType.TEXT_PLAIN_VALUE)
  public String ping() {
    return "ok";
  }

  @PostMapping("simulation/start")
  public String start(@RequestBody String json, @RequestParam(defaultValue = "false") boolean wait)
      throws SimulationException {
    String id = simulationController.start(json);
    if (wait) {
      simulationController.waitUntilComplete(id);
    }
    return id;
  }

  @GetMapping("simulation/status/{id}")
  public SimulationStatusDto status(@PathVariable String id) throws SimulationException {
    return simulationController.status(id);
  }

  @GetMapping(value = "simulation/results/{id}", produces = "application/zip")
  public void results(
      HttpServletResponse response,
      @PathVariable String id,
      @RequestParam(defaultValue = "true") boolean cleanup)
      throws SimulationException, IOException {
    response.setContentType("application/zip");
    response.setStatus(HttpServletResponse.SC_OK);
    simulationController.getZip(response.getOutputStream(), id);
    simulationController.cleanup(id);
  }
}
