package au.com.phiware.fuzzy;

/**
 * Provide a default implementation of {@code name} accessors and other {@code Object} methods.
 * 
 * @author Corin Lawson <corin@phiware.com.au>
 */
public abstract class AbstractNamedConcept implements NamedConcept {
	private String name;

	public AbstractNamedConcept(String name) {
		this.setName(name);
	}

	/* (non-Javadoc)
	 * @see au.com.phiware.fuzzy.NamedConcept#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see au.com.phiware.fuzzy.NamedConcept#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean equals(Object other) {
		if (other instanceof NamedConcept)
			return name.equals(((NamedConcept) other).getName());
		if (other instanceof String)
			return name.equals((String) other);
		return this == other;
	}

	public String toString() {
		return getName();
	}
}
