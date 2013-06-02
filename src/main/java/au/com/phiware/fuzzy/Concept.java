package au.com.phiware.fuzzy;

import java.util.Map;

/**
 * Represents a node in a {@link CognitiveMap} and encapsulates an activation
 * function.
 * Activation occurs in the context of a set of concepts and influences from
 * those concepts.
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public interface Concept {

	/**
	 * Computes the activation value from the given activations of concepts and
	 * their influence on this concept.
	 * @param activations of concepts of the FCM that this concept belongs to.
	 * @param influences on this concept of concepts of the FCM that this
	 *        concept belongs to.
	 * @return
	 */
	public double activation(Map<Concept, Double> activations, Map<Concept, Double> influences);

}