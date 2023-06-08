package org.vadere.restserver;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class TestResource {

  @GetMapping(value = "/ping", produces = MediaType.TEXT_PLAIN_VALUE)
  public String ping() {
    return "ok";
  }
}
