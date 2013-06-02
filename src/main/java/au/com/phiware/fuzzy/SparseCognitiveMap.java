package au.com.phiware.fuzzy;

import java.util.Set;

import au.com.phiware.math.random.RandomEngine;

import com.google.common.base.Function;
import com.google.common.collect.Table;

public class SparseCognitiveMap extends CognitiveMap {
	public static class Builder extends CognitiveMap.Builder {
		private final static RandomEngine<Double> random = RandomEngine.builder(Double.class).build();
		private double probabilityOfInhibit = 0.5;
		private double probabilityOfLink = 2.0/3;
	
		protected Builder() {
			super.setEdgeFunction(new Function<Table.Cell<Concept, Concept, Double>, Double>() {
				public Double apply(Table.Cell<Concept, Concept, Double> ignored) {
					return random.get() < probabilityOfLink
							? ( random.get() < probabilityOfInhibit ? -1.0 : 1.0 )
							: null;
				}
			});
			setTableFactory(new com.google.common.base.Supplier<Table<Concept, Concept, Double>>() {
				@Override public Table<Concept, Concept, Double> get() {
					return com.google.common.collect.HashBasedTable.create(concepts().size(), concepts().size());
				}
			});
		}
		public void setProbabilityOfInhibit(double probabilityOfInhibit) {
			this.probabilityOfInhibit = probabilityOfInhibit;
		}
		public void setProbabilityOfLink(double probabilityOfLink) {
			this.probabilityOfLink = probabilityOfLink;
		}
		@Override public SparseCognitiveMap build() {
			return build(new SparseCognitiveMap());
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public String toString() {
		Set<Concept> concepts = activations.keySet();
		StringBuilder sb = new StringBuilder();
		String buffer = "[";

		double v;
		for (Concept i : concepts) {
			sb.append(buffer).append(String.format("%.4f:", activations.get(i)));
			buffer = "[";
			for (Concept j : concepts) {
				v = edges.get(i, j);
				sb.append(buffer);
				if (v > 0)
					sb.append("↑");
				else if (v < 0)
					sb.append("↓");
				else
					sb.append(" ");
				buffer = "";
			}
			buffer = "] \n ";
		}
		sb.append( "]]" );
		return sb.toString();
	}
}
