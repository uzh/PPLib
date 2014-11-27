package ch.uzh.ifi.pdeboer.pplib.hcomp

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 27/11/14.
 */
class MultipleChoiceQueryTrivialTest {
	@Test
	def testHasTrivialSolutionMaxNumResults(): Unit = {
		val q = MultipleChoiceQuery("", List("test"), maxNumberOfResults = 1, minNumberOfResults = 1)
		val trivialAnswer: Option[HCompAnswer] = q.answerTrivialCases
		Assert.assertTrue(trivialAnswer.isDefined && trivialAnswer.get.as[MultipleChoiceAnswer].selectedAnswer == "test")
	}

	@Test
	def testHasNoTrivialSolution(): Unit = {
		val q = MultipleChoiceQuery("", List("test"), maxNumberOfResults = 1, minNumberOfResults = 0)
		Assert.assertFalse(q.answerTrivialCases.isDefined)
	}

}
