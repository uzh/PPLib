package ch.uzh.ifi.pdeboer.pplib.examples.optimization

import ch.uzh.ifi.pdeboer.pplib.hcomp.{MultipleChoiceAnswer, MultipleChoiceQuery}
import org.junit.{Assert, Test}

/**
  * Created by pdeboer on 11/12/15.
  */
class MCOptimizationMockPortalTest {
	@Test
	def testPortalSelections: Unit = {
		val portal = new MCOptimizationMockPortal()
		val options = "5,6,7,82".split(",").toList

		val iterations: Int = 10000
		val answers = (0 to iterations).map(i => portal.sendQueryAndAwaitResult(MultipleChoiceQuery("", options, 1)))
		val distribution = answers.groupBy(_.get.is[MultipleChoiceAnswer].selectedAnswer).map(g => (g._1, g._2.size))

		val max = distribution.maxBy(_._2)

		println(distribution)

		Assert.assertTrue(max._2 > iterations / 2)
		Assert.assertTrue(max._1 == "82")
	}
}
