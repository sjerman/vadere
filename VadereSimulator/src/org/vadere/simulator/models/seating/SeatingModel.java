package org.vadere.simulator.models.seating;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.log4j.Logger;
import org.vadere.simulator.control.ActiveCallback;
import org.vadere.simulator.models.Model;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.data.Table;

import com.vividsolutions.jts.math.MathUtil;

/**
 * This model can only be used with train scenarios complying with scenarios generated by Traingen.
 * 
 * To enable this model, add this model's class name to the main model's submodel list and
 * load a train topography.
 * 
 *
 */
public class SeatingModel implements ActiveCallback, Model {

	private final Logger log = Logger.getLogger(SeatingModel.class);
	
	private TrainModel trainModel;
	private Topography topography;
	private Random random;

	@Override
	public Map<String, Table> getOutputTables() {
		return new HashMap<>();
	}

	@Override
	public void preLoop(double simTimeInSec) {
		// before simulation
	}

	@Override
	public void postLoop(double simTimeInSec) {
		// after simulation
	}

	@Override
	public void update(double simTimeInSec) {
		final int seatCount = trainModel.getSeats().size();
		trainModel.getPedestrians().stream()
				.filter(p -> p.getTargets().isEmpty())
				.forEach(p -> p.getTargets().add(random.nextInt(seatCount)));
	}

	@Override
	public void initialize(List<Attributes> attributesList, Topography topography,
			AttributesAgent attributesPedestrian, Random random) {
		this.topography = topography;
		this.trainModel = new TrainModel(topography);
		this.random = random;
	}

	public TrainModel getTrainModel() {
		return trainModel;
	}

	public int chooseCompartment(Pedestrian person, int entranceAreaIndex) {
		// entrance areas:    0   1   2   3
		// compartments:    0   1   2   3   4
		// left- and rightmost compartments are "half compartments"

		final int entranceAreaCount = trainModel.getNumberOfEntranceAreas();

		final double distributionMean = entranceAreaIndex + 0.5;
		final double distributionSd = entranceAreaCount / 6.0;
		final RealDistribution distribution = new NormalDistribution(distributionMean, distributionSd);

		final double value = MathUtil.clamp(distribution.sample(), 0, entranceAreaCount);
		return (int) Math.round(value);
	}
	
	public int chooseSeatGroup(int compartmentIndex) {
		// TODO
		// get list of seat groups
		// get number of persons sitting there
		// choose according to some results of the study
		return new Random().nextInt(5);
	}
	
	public int chooseSeat(int seatGroupIndex) {
		final int personsSitting = 1; // TODO count persons sitting in seat group
		switch (personsSitting) {
		case 0:
			// TODO choose according to data
			return 0;

		case 1:
			// TODO choose according to data
			return 0;

		case 2:
			// TODO choose according to data
			// ignore features of persons
			// look at direction and side
			return 0;

		case 3:
			// TODO choose the seat left
			return 0;

		default:
			assert personsSitting == 4;
			throw new RuntimeException(String.format(
					"Seat group is already full. This method should not have been called!",
					personsSitting));
		}
	}

}
