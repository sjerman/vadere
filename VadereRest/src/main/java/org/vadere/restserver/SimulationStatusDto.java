package org.vadere.restserver;

import java.util.ArrayList;
import java.util.List;

public class SimulationStatusDto {

  @Override
  public String toString() {
    return "SimulationStatusDto [status=" + status + ", people=" + people + "]";
  }

  public static enum Status {
    PREPARING,
    RUNNING,
    FINISHED
  }

  public static class Person {
    private int src;
    private int target;
    private double x, y;

    public Person(int src, int target, double x, double y) {
      this.src = src;
      this.target = target;
      this.x = x;
      this.y = y;
    }

    @Override
    public String toString() {
      return "Person [src=" + src + ", target=" + target + ", x=" + x + ", y=" + y + "]";
    }

    public int getSrc() {
      return src;
    }

    public int getTarget() {
      return target;
    }

    public double getX() {
      return x;
    }

    public double getY() {
      return y;
    }
  }

  private Status status = Status.PREPARING;

  private List<Person> people = new ArrayList<>();

  public SimulationStatusDto(SimulationStatusDto status2) {
    this.status = status2.getStatus();
    this.people.addAll(status2.getPeople());
  }

  public SimulationStatusDto() {}

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public List<Person> getPeople() {
    return people;
  }

  public void setPeople(List<Person> people) {
    this.people = people;
  }
}
