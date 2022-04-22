package org.vadere.simulator.control.scenarioelements;

import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.simulator.models.groups.cgm.CentroidGroupModel;
import org.vadere.simulator.projects.dataprocessing.processor.util.ModelFilter;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Car;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Target;
import org.vadere.state.scenario.TargetListener;
import org.vadere.state.scenario.Topography;
import org.vadere.state.types.TrafficLightPhase;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TargetController extends ScenarioElementController implements ModelFilter {

	private static final Logger log = Logger.getLogger(TargetController.class);

	public final Target target;
	private Topography topography;

	public TrafficLightPhase phase = TrafficLightPhase.GREEN;

	public TargetController(Topography topography, Target target) {
		this.target = target;
		this.topography = topography;

		if (this.target.isStartingWithRedLight()) {
			phase = TrafficLightPhase.RED;
		} else {
			phase = TrafficLightPhase.GREEN;
		}
	}

	public void update(SimulationState state) {
		if (target.isTargetPedestrian()) {
			return;
		}

		for (DynamicElement element : getPrefilteredDynamicElements()) {

			final Agent agent;
			if (element instanceof Agent) {
				agent = (Agent) element;
			} else {
				log.error("The given object is not a subtype of Agent.");
				continue;
			}

			if (isNextTargetForAgent(agent)
					&& hasAgentReachedThisTarget(agent)) {

				notifyListenersTargetReached(agent);

				if (target.getWaitingTime() <= 0) {
					checkRemove(agent, state);
				} else {
					waitingBehavior(state, agent);
				}
			}
		}
	}

	private Collection<DynamicElement> getPrefilteredDynamicElements() {
		final double reachedDistance = target.getAttributes().getDeletionDistance();

		final Rectangle2D bounds = target.getShape().getBounds2D();
		final VPoint center = new VPoint(bounds.getCenterX(), bounds.getCenterY());
		final double radius = Math.max(bounds.getHeight(), bounds.getWidth()) + reachedDistance;

		final Collection<DynamicElement> elementsInRange = new LinkedList<>();
		elementsInRange.addAll(getObjectsInCircle(Pedestrian.class, center, radius));
		elementsInRange.addAll(getObjectsInCircle(Car.class, center, radius));
		
		return elementsInRange;
	}

	private void waitingBehavior(SimulationState state, final Agent agent) {
		final int agentId = agent.getId();
		// individual waiting behaviour, as opposed to waiting at a traffic light
		if (target.getAttributes().isIndividualWaiting()) {
			final Map<Integer, Double> enteringTimes = target.getEnteringTimes();
			if (enteringTimes.containsKey(agentId)) {
				if (state.getSimTimeInSec() - enteringTimes.get(agentId) > target
						.getWaitingTime()) {
					enteringTimes.remove(agentId);
					checkRemove(agent, state);
				}
			} else {
				final int parallelWaiters = target.getParallelWaiters();
				if (parallelWaiters <= 0 || (parallelWaiters > 0 &&
						enteringTimes.size() < parallelWaiters)) {
					enteringTimes.put(agentId, state.getSimTimeInSec());
				}
			}
		} else {
			// traffic light switching based on waiting time. Light starts green.
			phase = getCurrentTrafficLightPhase(state.getSimTimeInSec());

			if (phase == TrafficLightPhase.GREEN) {
				checkRemove(agent, state);
			}
		}
	}

	private <T extends DynamicElement> List<T> getObjectsInCircle(final Class<T> clazz, final VPoint center, final double radius) {
		return topography.getSpatialMap(clazz).getObjects(center, radius);
	}

	private boolean hasAgentReachedThisTarget(Agent agent) {
		final double reachedDistance = target.getAttributes().getDeletionDistance();
		final VPoint agentPosition = agent.getPosition();
		final VShape targetShape = target.getShape();

		return targetShape.contains(agentPosition)
				|| targetShape.distance(agentPosition) < reachedDistance;
	}

	private TrafficLightPhase getCurrentTrafficLightPhase(double simTimeInSec) {
		double phaseSecond = simTimeInSec % (target.getWaitingTime() * 2 + target.getWaitingTimeYellowPhase() * 2);

		if (target.isStartingWithRedLight()) {
			if (phaseSecond < target.getWaitingTime())
				return TrafficLightPhase.RED;
			if (phaseSecond < target.getWaitingTime() + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.YELLOW;
			if (phaseSecond < target.getWaitingTime() * 2 + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.GREEN;

			return TrafficLightPhase.YELLOW;
		} else {
			if (phaseSecond < target.getWaitingTime())
				return TrafficLightPhase.GREEN;
			if (phaseSecond < target.getWaitingTime() + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.YELLOW;
			if (phaseSecond < target.getWaitingTime() * 2 + target.getWaitingTimeYellowPhase())
				return TrafficLightPhase.RED;

			return TrafficLightPhase.YELLOW;
		}
	}

	private boolean isNextTargetForAgent(Agent agent) {
		boolean isNextTargetForAgent = false;

		if (agent.hasNextTarget()) {
			if (agent.getNextTargetId() == target.getId()
				&& agent.isCurrentTargetAnAgent() == false)
				isNextTargetForAgent = true;
		}

		return isNextTargetForAgent;
	}

	private void checkRemove(Agent agent, SimulationState state) {
		if (target.isAbsorbing()) {
			changeTargetOfFollowers(agent);
			topography.removeElement(agent);
		} else {
			if (agent instanceof Pedestrian) {
				Pedestrian p = (Pedestrian) agent;
				//TODO better GroupTargetController like GroupSourceController or make CentroidGroup a DynamicElement
				if (p.isAgentsInGroup()) {
					getModel(state, CentroidGroupModel.class).ifPresentOrElse(
							(model)
									-> {
								CentroidGroupModel cgm = (CentroidGroupModel) model;
								CentroidGroup group = cgm.getGroup(p);
								group.checkNextTarget(target.getNextSpeed());
							},
							()
									-> {
								log.error("no group Model found but Agent in Group");
							});
				} else {
					agent.checkNextTarget(target.getNextSpeed());
				}
			} else {
				agent.checkNextTarget(target.getNextSpeed());
			}
		}
	}

	// TODO [priority=high] [task=deprecation] removing the target from the list is deprecated, but still very frequently used everywhere.
	private void checkNextTarget(Agent agent) {
		final int nextTargetListIndex = agent.getNextTargetListIndex();

		// Deprecated target list usage
		if (nextTargetListIndex <= -1 && !agent.getTargets().isEmpty()) {
			agent.getTargets().removeFirst();
		}

		// The right way (later this first check should not be necessary anymore):
		if (agent.hasNextTarget()) {
				if (agent instanceof Pedestrian) {

					Pedestrian p = (Pedestrian) agent;
					if (p.isAgentsInGroup()) {
						//instead centroidGroup.changeTarget(nextIndex)
						p.incrementNextTargetListIndex();
						for (Pedestrian ped : p.getPedGroupMembers()) {
							p.incrementNextTargetListIndex();
						}
					} else {
						p.incrementNextTargetListIndex();
					}

				} else {
					agent.incrementNextTargetListIndex();
				}
		}

		// set a new desired speed, if possible. you can model street networks with differing
		// maximal speeds with this.
		if (target.getNextSpeed() >= 0) {
			agent.setFreeFlowSpeed(target.getNextSpeed());
		}
	}

	private void changeTargetOfFollowers(Agent agent) {
		for (Agent follower : agent.getFollowers()) {
			follower.setSingleTarget(target.getId(), false);
		}
		agent.getFollowers().clear();
	}

	private void notifyListenersTargetReached(final Agent agent) {
		for (TargetListener l : target.getTargetListeners()) {
			l.reachedTarget(target, agent);
		}
	}
	
}
