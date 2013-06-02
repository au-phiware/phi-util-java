package au.com.phiware.fuzzy;

import static java.lang.Double.POSITIVE_INFINITY;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.*;

import org.junit.Test;

public class NormalizedForcedConceptTest {
	double DELTA = 1e-300;

	@Test
	public void testNormalize() {
		double[] fixtures = {-2, -1.75, -1.5, -1.25, -1, -0.75, -0.5, -0.25, 0, 0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2};
		for (double f : fixtures)
			assertThat(new NormalizedForcedConcept(POSITIVE_INFINITY, 0.5, 0.75).normalize(f),
						closeTo(1.0, DELTA));
	}

	@Test
	public void testDefault() {
		double[] fixtures = {-2, -1.75, -1.5, -1.25, -1, -0.75, -0.5, -0.25, 0, 0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2};
		for (double f : fixtures)
			assertThat(new NormalizedForcedConcept().normalize(f),
					closeTo(new NormalizedConcept().normalize(f), DELTA));
	}
}
