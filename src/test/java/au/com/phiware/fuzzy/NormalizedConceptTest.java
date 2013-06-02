package au.com.phiware.fuzzy;

import static java.lang.Double.POSITIVE_INFINITY;
import static org.junit.Assert.assertThat;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.AnyOf.anyOf;

import org.junit.Test;

public class NormalizedConceptTest {
	double DELTA = 1e-300;

	@Test
	public void testMiddle() {
		double[] fixtures = {-2, -1.75, -1.5, -1.25, -1, -0.75, -0.5, -0.25, 0, 0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2};
		for (double f : fixtures)
			for (double g: fixtures)
				assertThat((new NormalizedConcept(g, f)).normalize(f), closeTo(0.5, DELTA));
	}

	@Test
	public void testRange() {
		int i = 1000;
		while (i-- != 0) {
			double f = Math.random() * 2 - 1,
					g = Math.random() * 2 - 1,
					h = Math.random() * 2 - 1;
			assertThat((new NormalizedConcept(g, f)).normalize(h), allOf(greaterThanOrEqualTo(0.0), lessThanOrEqualTo(1.0)));
		}
		i = 1000;
		while (i-- != 0) {
			double f = Math.random() * 20 - 10,
					g = Math.random() * 20 - 10,
					h = Math.random() * 20 - 10;
			assertThat((new NormalizedConcept(g, f)).normalize(h), allOf(greaterThanOrEqualTo(0.0), lessThanOrEqualTo(1.0)));
		}
		i = 1000;
		while (i-- != 0) {
			double f = Math.random() * 200 - 100,
					g = Math.random() * 200 - 100,
					h = Math.random() * 200 - 100;
			assertThat((new NormalizedConcept(g, f)).normalize(h), allOf(greaterThanOrEqualTo(0.0), lessThanOrEqualTo(1.0)));
		}
	}

	@Test
	public void testSquare() {
		int i = 1000;
		while (i-- != 0) {
			double f = Math.random() * 2 - 1,
					h = Math.random() * 2 - 1;
			assertThat((new NormalizedConcept(POSITIVE_INFINITY, f)).normalize(h), anyOf(closeTo(0.0, DELTA), closeTo(1.0, DELTA)));
		}
		i = 1000;
		while (i-- != 0) {
			double f = Math.random() * 20 - 10,
					h = Math.random() * 20 - 10;
			assertThat((new NormalizedConcept(POSITIVE_INFINITY, f)).normalize(h), anyOf(closeTo(0.0, DELTA), closeTo(1.0, DELTA)));
		}
		i = 1000;
		while (i-- != 0) {
			double f = Math.random() * 200 - 100,
					h = Math.random() * 200 - 100;
			assertThat((new NormalizedConcept(POSITIVE_INFINITY, f)).normalize(h), anyOf(closeTo(0.0, DELTA), closeTo(1.0, DELTA)));
		}
	}
}
