package org.vadere.util.math;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public class TruncatedNormalDistribution extends NormalDistribution {

	private static final long serialVersionUID = 1L;
	
	private double min;
	private double max;
	
	private int maxIterations;

	public TruncatedNormalDistribution(RandomGenerator rng, double mean, double standardDeviation, double min,
			double max, int maxIterations) {
		super(rng, mean, standardDeviation);

		if (min < 0 || max <= 0 || max <= min){
			// min == 0 does not make to much sense either, but we allow this case for pedestrians that do not move
			throw new IllegalArgumentException("Parameters 'min' and 'max' must be non-negative and 'min < max'.");
		}

		this.min = min;
		this.max = max;
		this.maxIterations = maxIterations;
	}

	@Override
	public double sample() {
		for (int i = 0; i < maxIterations; i++) {
			double sample = super.sample();
			if (sample >= min && sample <= max)
				return sample;
		}
		throw new IllegalArgumentException(
				"Max iteration count reached on sampling for truncated distribution. Parameters bound and min are not suitable.");
	}

}