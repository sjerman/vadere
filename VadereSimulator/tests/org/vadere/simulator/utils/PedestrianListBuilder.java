package org.vadere.simulator.utils;

import org.vadere.simulator.projects.dataprocessing.datakey.OverlapData;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.VTrajectory;
import org.vadere.util.geometry.shapes.VPoint;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class PedestrianListBuilder {

	private ArrayList<Pedestrian> out;
	private Pedestrian p;

	public PedestrianListBuilder() {
		this.out = new ArrayList<>();
	}

	public PedestrianListBuilder add(int id) {
		out.add(new Pedestrian(new AttributesAgent(id), new Random()));
		return this;
	}

	public PedestrianListBuilder add(int id, VPoint pos) {
		Pedestrian p = new Pedestrian(new AttributesAgent(id), new Random());
		p.setPosition(pos);
		out.add(p);
		return this;
	}

	public PedestrianListBuilder add(Integer... ids) {
		for (Integer id : ids) {
			Pedestrian p = new Pedestrian(new AttributesAgent(id), new Random());
			out.add(p);
		}
		return this;
	}

	public PedestrianListBuilder add(int id, VTrajectory trajectory){
		Pedestrian p = new Pedestrian(new AttributesAgent(id), new Random());
		Class<?> pclass = p.getClass();

		// Use reflection to set the trajectory forcefully (this bypasses a
		// "public setTrajectory" method in Pedestrian.class)
		try{
			// Fails if the trajectory attribute is renamed:
			Field trajectoryField = pclass.getDeclaredField("trajectory");
			trajectoryField.setAccessible(true);  // trajectory is private --> we set it to accessible here
			trajectoryField.set(p, trajectory);
		}catch (NoSuchFieldException | IllegalAccessException ex){
			ex.printStackTrace();
		}
		out.add(p);
		return this;
	}

	public PedestrianListBuilder clear() {
		out = new ArrayList<>();
		return this;
	}

	public double getDistByPedId(int id1, int id2){
		VPoint p1 = out.stream().filter(p -> p.getId() == id1).findFirst().get().getPosition();
		VPoint p2 = out.stream().filter(p -> p.getId() == id2).findFirst().get().getPosition();
		return p1.distance(p2);
	}

	public OverlapData overlapData(int id1, int id2, double minDist){
		return new OverlapData(out.get(id1 -1) , out.get(id2 -1), minDist);
	}

	public List<Pedestrian> getList() {
		return out;
	}

	public List<Agent> getAgentList() {
		return new LinkedList<>(out);
	}

	public List<DynamicElement> getDynamicElementList(){
		return new LinkedList<>(out);
	}
}
