package ch.uzh.ifi.pdeboer.pplib.examples.optimization

import org.junit.{After, Before, Assert, Test}

/**
  * Created by pdeboer on 21/12/15.
  */
class MCOptimizationResultTest {
	@Before
	def setConstant: Unit = {
		MCOptimizeConstants.multipeChoiceAnswers = "10,10,10,70"
	}

	@Test
	def answerDistance: Unit = {
		Assert.assertEquals(0, MCOptimizeConstants.answerDistance(70))
		Assert.assertEquals(60, MCOptimizeConstants.answerDistance(10))
	}

	@After
	def resetConstant: Unit = {
		MCOptimizeConstants.multipeChoiceAnswers = ""
	}
}
