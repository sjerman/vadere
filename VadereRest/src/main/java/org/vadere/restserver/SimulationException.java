package org.vadere.restserver;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Bad simulation")
public class SimulationException extends Exception {

  public SimulationException(String reason) {
    super(reason);
  }

  public SimulationException(String reason, Exception e) {
    super(reason, e);
  }
}
