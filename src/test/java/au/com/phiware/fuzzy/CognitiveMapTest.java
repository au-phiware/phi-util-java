package au.com.phiware.fuzzy;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import org.junit.Test;

public class CognitiveMapTest extends CognitiveMap {

	@Test
	public final void testBuilderReuse() {
		Concept[] concepts = new Concept[] {
			new NormalizedConcept(),
			new NormalizedConcept(),
			new NormalizedConcept(),
			new NormalizedConcept(),
			new NormalizedConcept()
		};
		CognitiveMap.Builder builder = CognitiveMap.builder()
				.addAllConcepts(concepts)
				.putEdge(concepts[0], concepts[1], 0.2)
				.putEdge(concepts[0], concepts[2], 0.9)
				.putEdge(concepts[1], concepts[3], 0.5)
				.putEdge(concepts[1], concepts[4], 0.25)
				.putEdge(concepts[2], concepts[4], 0.3);
		CognitiveMap a = builder.build(),
				b = builder.build();

		assertThat(a.toString(), is(b.toString()));
		assertThat(a.activations, is(b.activations));
		assertThat(a.activations, not(sameInstance(b.activations)));
		assertThat(a.edges, is(b.edges));
		assertThat(a.edges, not(sameInstance(b.edges)));
	}

	@Test
	public final void testBuilderRandomiser() {
		Concept[] concepts = new Concept[] {
			new NormalizedConcept(),
			new NormalizedConcept(),
			new NormalizedConcept(),
			new NormalizedConcept(),
			new NormalizedConcept()
		};
		CognitiveMap.Builder builder = CognitiveMap.builder()
				.addAllConcepts(concepts)
				.randomizingEdgeFunction();
		CognitiveMap a = builder.build(),
				b = builder.build();

		assertThat(a.toString(), not(b.toString()));
	}
}
