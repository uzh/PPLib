package ch.uzh.ifi.pdeboer.pplib.util

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 03/11/14.
 */
class MonteCarloTest {
	@Test
	def testMonteCarlo95Confidence: Unit = {
		Assert.assertEquals(Some(7), MonteCarlo.minAgreementRequired(5, 12, 0.95, 100000))
	}

	@Test
	def testMonteCarlo90Confidence: Unit = {
		Assert.assertEquals(Some(6), MonteCarlo.minAgreementRequired(5, 12, 0.90, 100000))

	}
}
