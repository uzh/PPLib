package ch.uzh.ifi.pdeboer.pplib.util

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 12/11/14.
 */
class UTest {
	@Test
	def testRetry: Unit = {
		var testIteration = 3

		U.retry(testIteration) {
			testIteration -= 1
			if (testIteration > 1) throw new IllegalArgumentException("exception should not be relayed")
		}
		Assert.assertEquals(1, testIteration)
	}
}
