package au.com.phiware.fuzzy;

/**
 * Simply associates a label to a concept.s
 *
 * @author Corin Lawson <corin@phiware.com.au>
 */
public interface NamedConcept extends Concept {
	/**
	 * @return the name that this concept represents.
	 */
	public String getName();

	/**
	 * @param name that this concept represents.
	 */
	public void setName(String name);
}