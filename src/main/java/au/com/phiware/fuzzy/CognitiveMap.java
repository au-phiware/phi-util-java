package au.com.phiware.fuzzy;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Maps.filterEntries;
import static com.google.common.collect.Maps.transformEntries;
import static java.lang.Double.NaN;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import au.com.phiware.math.random.RandomEngine;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ArrayTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

public class CognitiveMap {
	protected Map<Concept, Double> activations;
	protected Table<Concept, Concept, Double> edges;
	protected double sensitivity = 0; 

	public static class Builder {
		private Supplier<Map<Concept, Double>> mapFactory = new Supplier<Map<Concept,Double>>() {
			@Override public Map<Concept, Double> get() {
				return new HashMap<>();
			}
		};
		private Supplier<Table<Concept, Concept, Double>> tableFactory = new Supplier<Table<Concept, Concept, Double>>() {
			@Override public Table<Concept, Concept, Double> get() {
				return ArrayTable.create(concepts(), concepts());
			}
		};
		private Set<Concept> concepts;
		private Map<Concept, Double> activations;
		private Table<Concept, Concept, Double> edges;
		private Function<?, Double> initEdgeFunction;
		private Function<?, Double> initActivationFunction = Functions.constant(Double.valueOf(0));
		private double sensitivity = 1e-4;

		public double getSensitivity() {
			return sensitivity;
		}

		public void setSensitivity(double sensitivity) {
			this.sensitivity = sensitivity;
		}

		public Builder concat(CognitiveMap map) {
			concepts().addAll(map.activations.keySet());
			activations().putAll(map.activations);
			edges().putAll(map.edges);
			return this;
		}

		public Builder clearConcepts() {
			concepts = null;
			activations = null;
			edges = null;
			return this;
		}
		protected Set<Concept> concepts() {
			if (concepts == null) concepts = new HashSet<>();
			return concepts;
		}

		protected Map<Concept, Double> activations() {
			if (activations == null) activations = mapFactory.get();
			return activations;
		}

		protected Table<Concept, Concept, Double> edges() {
			if (edges == null)
				edges = tableFactory.get();
			return edges;
		}

		public Builder setMapFactory(Supplier<Map<Concept, Double>> factory) {
			checkNotNull(factory);
			this.mapFactory = factory;
			return this;
		}

		public Builder setTableFactory(Supplier<Table<Concept, Concept, Double>> factory) {
			checkNotNull(factory);
			this.tableFactory = factory;
			return this;
		}

		public Builder setActivationFunction(Function<Concept, Double> function) {
			checkNotNull(function);
			initActivationFunction = function;
			return this;
		}

		public Builder setEdgeFunction(@Nullable Function<Table.Cell<Concept, Concept, Double>, Double> function) {
			initEdgeFunction = function;
			return this;
		}

		public Builder randomizingEdgeFunction() {
			initEdgeFunction = randomiser;
			return this;
		}

		public Builder addConcept(Concept concept) {
			checkNotNull(concept);
			concepts().add(concept);
			return this;
		}
		
		public Builder addAllConcepts(Concept... concepts) {
			checkNotNull(concepts);
			concepts().addAll(Arrays.asList(concepts));
			return this;
		}

		public Builder addAllConcepts(Collection<? extends Concept> concepts) {
			checkNotNull(concepts);
			concepts().addAll(concepts);
			return this;
		}

		public Builder addConcept(Concept concept, double initialActivation) {
			checkNotNull(concept);
			concepts().add(concept);
			activations().put(concept, initialActivation);
			return this;
		}

		public Builder addAllConcepts(Map<? extends Concept, Double> initialActivation) {
			checkNotNull(initialActivation);
			concepts().addAll(initialActivation.keySet());
			activations().putAll(initialActivation);
			return this;
		}

		/**
		 * It may be important to ensure that all Concepts have been added
		 * before putting any edge, depending on the tableFactory of this
		 * Builder. This is true of the default implementation of
		 * tableFactory, unless you only have a single edge.
		 * 
		 * @param from
		 * @param to
		 * @param weight
		 * @return
		 */
		public Builder putEdge(Concept from, Concept to, double weight) {
			checkNotNull(from);
			checkNotNull(to);
			concepts().add(from);
			concepts().add(to);
			edges().put(from, to, weight);
			return this;
		}

		/**
		 * It may be important to ensure that all Concepts have been added
		 * before putting any edge, depending on the tableFactory of this
		 * Builder. This is true of the default implementation of
		 * tableFactory, unless no other edges will be put into this
		 * Builder.
		 * 
		 * @param edges
		 * @return
		 */
		public Builder putAllEdges(Table<? extends Concept, ? extends Concept, Double> edges) {
			checkNotNull(edges);
			concepts().addAll(edges.rowKeySet());
			concepts().addAll(edges.columnKeySet());
			edges().putAll(edges);
			return this;
		}

		public CognitiveMap build() {
			return build(new CognitiveMap());
		}
		protected <T extends CognitiveMap> T build(T map) {
			if (concepts == null || concepts.isEmpty()) {
				map.activations = Collections.emptyMap();
				map.edges = ImmutableTable.of();
			} else {
				map.activations = mapFactory.get();
				if (activations == null) {
					for (Concept c : concepts)
						map.activations.put(c, initActivationFunction().apply(c));
				} else {
					for (Concept c : concepts)
						if (!activations.containsKey(c))
							map.activations.put(c, initActivationFunction().apply(c));
						else
							map.activations.put(c, activations.get(c));
				}

				boolean empty = edges == null || edges.isEmpty();
				if (initEdgeFunction == null && empty) {
					map.edges = ImmutableTable.of();
				} else {
					Double v = null;
					map.edges = tableFactory.get();
					for (Concept i : concepts)
						for (Concept j : concepts)
							if ((!empty && (v = edges.get(i, j)) != null)
								|| (initEdgeFunction != null
										&& (v = initEdgeFunction().apply(Tables.immutableCell(i, j, NaN))) != null))
								map.edges.put(i, j, v);
				}
			}
			map.sensitivity = sensitivity;
			return map;
		}

		@SuppressWarnings("unchecked")
		private Function<Concept, Double> initActivationFunction() {
			return (Function<Concept, Double>) initActivationFunction;
		}

		@SuppressWarnings("unchecked")
		private Function<Table.Cell<Concept, Concept, Double>, Double> initEdgeFunction() {
			return (Function<Table.Cell<Concept, Concept, Double>, Double>) initEdgeFunction;
		}

		private static final Function<Object, Double> randomiser
			= new Function<Object, Double>() {
				private final RandomEngine<Double> random = RandomEngine.builder(Double.class).build();
				public Double apply(Object ignored) {
					return random.get() * 2 - 1;
				}
			};
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	protected CognitiveMap() {}

	public static CognitiveMap copyOf(CognitiveMap map) {
		CognitiveMap rv = new CognitiveMap();
		
		rv.activations = ImmutableMap.copyOf(map.activations);
		rv.edges = ImmutableTable.copyOf(map.edges);
		
		return rv;
	}

	/**
	 * @return the edges matrix
	 */
	public ImmutableTable<Concept, Concept, Double> getEdges() {
		return ImmutableTable.copyOf(edges);
	}

	/**
	 * @return the value of the first concept found with the specified name. 
	 * @throws ArrayIndexOutOfBoundsException if specified name is not a concept.
	 */
	public double getConceptValue(Concept concept) {
		return activations.get(concept);
	}

	private Predicate<Map.Entry<Concept, Double>> sense = new Predicate<Map.Entry<Concept, Double>>() {
		@Override public boolean apply(@Nullable Map.Entry<Concept, Double> entry) {
			return entry != null && entry.getKey() != null && entry.getValue() != null
					&& entry.getValue() > sensitivity;
		}
	};
	public void step() {
		final Map<Concept, Double> activations = this.activations;
		this.activations = ImmutableMap.copyOf(
			transformEntries(activations, new Maps.EntryTransformer<Concept, Double, Double>() {
				public Double transformEntry(Concept c, Double ignored) {
					return c.activation(
							filterEntries(activations, sense),
							filterEntries(edges.row(c), sense)
					);
				}
			})
		);
		//System.out.println(this.toString()+"\n");
	}

	public String toString() {
		Set<Concept> concepts = activations.keySet();
		StringBuilder sb = new StringBuilder( "FCM for " + activations + ".\n" );
		String buffer = "[";

		for(Concept concept : concepts) {
			sb.append( buffer + concept + " = " + String.format( "%.4f", activations.get(concept) ) );
			buffer = ", ";
		}
		buffer = "]\n[";
		for (Concept i : concepts) {
			sb.append( buffer );
			buffer = "[";
			for (Concept j : concepts) {
				Double v = edges.get(i, j);
				sb.append( buffer + String.format( "% .1f", v != null ? v : 0 ) );
				buffer = ", ";
			}
			buffer = "] \n ";
		}
		sb.append( "]]" );
		return sb.toString();
	}
}
