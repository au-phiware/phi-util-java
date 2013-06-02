package au.com.phiware.fuzzy;

public class NormalizedForcedConcept extends NormalizedConcept {
	private double forcedValue = Double.NEGATIVE_INFINITY;

	public NormalizedForcedConcept() {
		super();
	}
	public NormalizedForcedConcept(double slope) {
		super(slope);
	}
	public NormalizedForcedConcept(double slope, double middle) {
		super(slope, middle);
	}
	public NormalizedForcedConcept(double slope, double middle, double forced) {
		super(slope, middle);
		setForcedActivationValue(forced);
	}

	/**
	 * @param forcedValue the forced activation value
	 */
	public double getForcedActivationValue() {
		return forcedValue;
	}

	/**
	 * @param forcedValue the forced activation value
	 */
	public void setForcedActivationValue(double forcedValue) {
		this.forcedValue = forcedValue;
	}

	/**
	 * Returns the forced activation value used during normalize.
	 * Default implementation returns the forcedActivationValue of this Concept (identity).
	 * @return the forced activation value
	 */
	public double forcedActivationValue() {
		return getForcedActivationValue();
	}

	/**
	 * Combine forced value and unnormalized activation value.
	 * Default implementation returns the greater of the two arguments (max).
	 */
	public double force(double forcedValue, double value) {
		return forcedValue > value ? forcedValue : value;
	}

	@Override
	public double normalize(double activation) {
		return super.normalize(force(forcedActivationValue(), activation));
	}
}
