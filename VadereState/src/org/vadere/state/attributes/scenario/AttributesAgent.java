package org.vadere.state.attributes.scenario;

/**
 * Provides attributes for an agent, like body radius, ...
 * 
 * TODO [priority=low] Create two Attributes for better performance: Common
 * Attributes and individual Attributes for pedestrians.
 * 
 * 
 */
public class AttributesAgent extends AttributesDynamicElement {

	// from weidmann-1992 page 18, deviates in seitz-2016c page 2 (Methods): 2.0
	private double radius = 0.2;

	// use a weidmann speed adjuster, this is not implemented jet => only densityDependentSpeed = false works.
	private boolean densityDependentSpeed = false;

	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private double speedDistributionMean = 1.34;

	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private double speedDistributionStandardDeviation = 0.26;

	// from weidmann-1992 page 18, seitz-2016c page 2 (Methods)
	private double minimumSpeed = 0.5;

	// from weidmann-1992 page 18, deviates in seitz-2016c page 2 (Methods): 2.0
	private double maximumSpeed = 2.2;

	// only used for the GNM and SFM
	private double acceleration = 2.0;

	// store n last foot steps for speed calculation
	private int footStepsToStore = 10;

	// agents search for other scenario elements (e.g., other agents) within this radius
	private double searchRadius = 1.0;

	public AttributesAgent() {
		this(-1);
	}

	public AttributesAgent(final int id) {
		super(id);
	}

	/**
	 * Copy constructor with new id assignment.
	 */
	public AttributesAgent(final AttributesAgent other, final int id) {
		super(id);
		this.radius = other.radius;
		this.densityDependentSpeed = other.densityDependentSpeed;
		this.speedDistributionMean = other.speedDistributionMean;
		this.speedDistributionStandardDeviation = other.speedDistributionStandardDeviation;
		this.minimumSpeed = other.minimumSpeed;
		this.maximumSpeed = other.maximumSpeed;
		this.acceleration = other.acceleration;
		this.footStepsToStore = other.footStepsToStore;
		this.searchRadius = other.searchRadius;
	}

	// Getters...

	public double getRadius() {
		return radius;
	}

	public boolean isDensityDependentSpeed() {
		return densityDependentSpeed;
	}

	public double getSpeedDistributionMean() {
		return speedDistributionMean;
	}

	public double getSpeedDistributionStandardDeviation() {
		return speedDistributionStandardDeviation;
	}

	public double getMinimumSpeed() {
		return minimumSpeed;
	}

	public double getMaximumSpeed() {
		return maximumSpeed;
	}

	public double getAcceleration() {
		return acceleration;
	}

	public int getFootStepsToStore() { return footStepsToStore; }

	public double getSearchRadius() { return searchRadius; }

	// Setters...

	public void setRadius(double radius) {
		checkSealed();
		this.radius = radius;
	}

	public void setDensityDependentSpeed(boolean densityDependentSpeed) {
		checkSealed();
		this.densityDependentSpeed = densityDependentSpeed;
	}

	public void setSpeedDistributionMean(double speedDistributionMean) {
		checkSealed();
		this.speedDistributionMean = speedDistributionMean;
	}

	public void setSpeedDistributionStandardDeviation(double speedDistributionStandardDeviation) {
		checkSealed();
		this.speedDistributionStandardDeviation = speedDistributionStandardDeviation;
	}

	public void setMinimumSpeed(double minimumSpeed) {
		checkSealed();
		this.minimumSpeed = minimumSpeed;
	}

	public void setMaximumSpeed(double maximumSpeed) {
		checkSealed();
		this.maximumSpeed = maximumSpeed;
	}

	public void setAcceleration(double acceleration) {
		checkSealed();
		this.acceleration = acceleration;
	}

	public void setFootStepsToStore(int footStepsToStore) {
		checkSealed();
		this.footStepsToStore = footStepsToStore;
	}

	public void setSearchRadius(double searchRadius) {
		checkSealed();
		this.searchRadius = searchRadius;
	}
}
