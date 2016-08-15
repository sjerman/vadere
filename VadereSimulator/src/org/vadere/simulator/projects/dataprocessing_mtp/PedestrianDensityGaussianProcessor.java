package org.vadere.simulator.projects.dataprocessing_mtp;

public class PedestrianDensityGaussianProcessor extends PedestrianDensityProcessor {

	@Override
	void init(final AttributesProcessor attributes, final ProcessorFactory factory) {
		AttributesPedestrianDensityGaussianProcessor attDensGauss =
				(AttributesPedestrianDensityGaussianProcessor) attributes;
		this.setAlgorithm(new PointDensityGaussianAlgorithm(attDensGauss.getScale(), attDensGauss.getStandardDerivation(),
				attDensGauss.isObstacleDensity()));

		super.init(attributes, factory);
	}
}
