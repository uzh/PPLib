package ch.uzh.ifi.pdeboer.pplib.patterns

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 30/11/14.
 */
class IterativeRefinementTest {
	@Test
	def testExecutor(): Unit = {
		val text = "asdf"
		val exec = new IterativeRefinementExecutor(text, new SimpleIRDriver(), 5, stringDifferenceThreshold = 0)

		Assert.assertEquals("asdf11111", exec.refinedText)
	}

	@Test
	def testExecutorWithIterationWatcher(): Unit = {
		val text = "asdf"
		val exec = new IterativeRefinementExecutor(text, new SimpleIRDriver(), 5, stringDifferenceThreshold = 1)

		Assert.assertEquals("asdf11", exec.refinedText)
	}

	class SimpleIRDriver extends IterativeRefinementDriver[String] {
		override def refine(originalToRefine: String, refinementState: String, iteration: Int): String = refinementState + "1"

		override def selectBestRefinement(candidates: List[String]): String = candidates.maxBy(_.length)
	}

}
