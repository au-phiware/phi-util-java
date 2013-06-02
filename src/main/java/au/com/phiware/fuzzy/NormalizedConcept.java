package au.com.phiware.fuzzy;

import static com.google.common.collect.Sets.intersection;
import static java.lang.Math.exp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Map;

/**
 * Provides a normalized (in the range of {@code [0, 1]}) activation function.
 * This implementation uses the logistic curve to restrict the activation value
 * to the unit interval.
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public class NormalizedConcept implements Concept {
	private final static Logger logger = LoggerFactory.getLogger(NormalizedConcept.class);
	private final static Marker marker = MarkerFactory.getMarker("UNNORMALIZED_VALUE");
	private double squashingMiddle;
	private double squashingSlope;

	/**
	 * Constructs a logistic curve centered about 0.5 with slope of 2.
	 */
	public NormalizedConcept() {
		this(2.0);
	}

	/**
	 * Constructs logistic curve centered about 0.5 with specified slope.
	 * 
	 * @param slope of logistic curve.
	 */
	public NormalizedConcept(double slope) {
		this(slope, 0.5);
	}

	/**
	 * Constructs a logistic curve with specified slope and middle.
	 * 
	 * @param slope of logistic curve.
	 * @param middle of logistic curve.
	 */
	public NormalizedConcept(double slope, double middle) {
		squashingMiddle = middle;
		squashingSlope = slope;
	}

	/**
	 * Returns the middle point of the normalize function.
	 * @return the middle point of the normalize function.
	 */
	public double getSquashingMiddle() {
		return squashingMiddle;
	}

	/**
	 * Returns the slope of the normalize function.
	 * @return the slope of the normalize function.
	 */
	public double getSquashingSlope() {
		return squashingSlope;
	}

	/**
	 * Normalizes the specified value.
	 * @param value to normalize.
	 * @return the value normalized to the range {@code [0,1]}.
	 */
	public double normalize(double value) {
		double k, a0 = getSquashingMiddle();
		if( ( k = getSquashingSlope() ) == Double.POSITIVE_INFINITY )
			return value <= a0 ? 0 : 1;
		return 1.0 / ( 1.0 + exp( -k * ( value - a0 ) ) );
	}

	public double activation(Map<Concept, Double> c, Map<Concept, Double> e) {
		double aggregate = 0;
		for (Concept i : intersection(c.keySet(), e.keySet()))
			aggregate += c.get(i) * e.get(i);
		logger.info(marker, "{}", aggregate);
		return normalize(aggregate);
	}
}