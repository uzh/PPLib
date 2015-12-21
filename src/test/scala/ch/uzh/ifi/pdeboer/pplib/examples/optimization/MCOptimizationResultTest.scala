package ch.uzh.ifi.pdeboer.pplib.examples.optimization

import org.junit.{Assert, Test}

/**
  * Created by pdeboer on 21/12/15.
  */
class MCOptimizationResultTest {
	@Test
	def answerDistance: Unit = {
		Assert.assertEquals(0, MCOptimizeConstants.answerDistance(70))
		Assert.assertEquals(60, MCOptimizeConstants.answerDistance(10))
	}
}
