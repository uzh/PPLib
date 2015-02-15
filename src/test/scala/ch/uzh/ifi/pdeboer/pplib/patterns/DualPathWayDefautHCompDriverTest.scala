package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.IndexedPatch
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 29/10/14.
 */
class DualPathWayDefaultHCompDriverComparisonTest {
	val portal = new MockHCompPortal()
	val emptyQ: HCompInstructionsWithTupleStringified = HCompInstructionsWithTupleStringified("")
	val data = List("a", "b", "c", "d").zipWithIndex.map(d => new IndexedPatch(d))

	@Test
	def testComparisonAndAdvance(): Unit = {
		val ADVANCE_IS_OK: String = "AdvanceIsOK"
		val f = (q: HCompQuery) => {
			if (q.question.contains(ADVANCE_IS_OK))
				Some(MultipleChoiceAnswer(
					q.asInstanceOf[MultipleChoiceQuery],
					Map("Yes" -> true, "No" -> false)
				))
			else None
		}
		portal.filters ::= f
		val driver = newDriver(ADVANCE_IS_OK)

		val pathway = driver.indexMap.map(i => DPChunk(i._1, i._2, i._2.value)).toList

		val ret = driver.comparePathwaysAndDecideWhetherToAdvance(
			pathway, pathway
		)

		Assert.assertTrue(ret)
	}

	@Test
	def testComparisonAndDontAdvance(): Unit = {
		val ADVANCE_NOT_OK: String = "AdvanceNotOK"
		val f = (q: HCompQuery) => {
			if (q.question.contains(ADVANCE_NOT_OK))
				Some(MultipleChoiceAnswer(
					q.asInstanceOf[MultipleChoiceQuery],
					Map("Yes" -> false, "No" -> true)
				))
			else None
		}
		portal.filters ::= f
		val driver = newDriver(ADVANCE_NOT_OK)

		val pathway = driver.indexMap.map(i => DPChunk(i._1, i._2, i._2.value)).toList

		val ret = driver.comparePathwaysAndDecideWhetherToAdvance(
			pathway, pathway
		)

		Assert.assertFalse(ret)
	}

	def newDriver(text: String) =
		new DualPathWayDefaultHCompDriver(data, portal, emptyQ, emptyQ, "",
			new DPHCompDriverDefaultComparisonInstructionsConfig(text, positiveAnswerForComparison = "Yes", negativeAnswerForComparison = "No"))
}

class DualPathWayDefaultHCompDriverProcessingTest {
	val portal = new MockHCompPortal()
	portal.filters = List(
		createFilterRule("a", "b"), createFilterRule("b", "c"), createFilterRule("c", "d"),
		createFilterRule("d", "e")
	)
	val data = List("a", "b", "c", "d").zipWithIndex.map(d => new IndexedPatch(d))
	val emptyQ: HCompInstructionsWithTupleStringified = HCompInstructionsWithTupleStringified("")

	@Test
	def testProcessingUnitAllCorrectInclNewItem(): Unit = {
		val driver = newDriver
		val ret = driver.processChunksAndPossiblyAddNew(
			List(
				DPChunk(1, new IndexedPatch("b", 1), "c"), DPChunk(0, new IndexedPatch("a", 0), "b")),
			Some(2)
		)

		Assert.assertEquals(Set("b", "c", "d"), ret.map(_.answer).toSet)
	}

	@Test
	def testProcessingUnitErrorCorrectionInclNewItem(): Unit = {
		val driver = newDriver

		portal.filters ::= createFilterRule("b", "WRONG")
		portal.filters ::= createFilterRule("a", "WRONG2")

		val retFixedError = driver.processChunksAndPossiblyAddNew(
			List(
				DPChunk(1, new IndexedPatch("b", 1), "c", "WRONG"), DPChunk(0, new IndexedPatch("a", 0), "b", "WRONG2")),
			Some(2)
		)
		Assert.assertEquals(List("b", "c", "d").toSet, retFixedError.map(_.answer).toSet)
	}

	@Test
	def testProcessingUnitAllCorrectExclNewItem(): Unit = {
		val driver = newDriver
		val ret = driver.processChunksAndPossiblyAddNew(
			List(
				DPChunk(1, new IndexedPatch("b", 1), "c"), DPChunk(0, new IndexedPatch("a", 0), "b")))

		Assert.assertEquals(List("b", "c").toSet, ret.map(_.answer).toSet)
	}

	@Test
	def testProcessingUnitErrorCorrectionExclNewItem(): Unit = {
		val driver = newDriver

		val retFixedError = driver.processChunksAndPossiblyAddNew(
			List(
				DPChunk(1, data(1), "c", "WRONG"), DPChunk(0, data(0), "b", "WRONG2")))
		Assert.assertEquals(List("b", "c").toSet, retFixedError.map(_.answer).toSet)
	}


	def newDriver = new DualPathWayDefaultHCompDriver(data, portal, emptyQ, emptyQ, "",
		new DPHCompDriverDefaultComparisonInstructionsConfig("", positiveAnswerForComparison = "Yes", negativeAnswerForComparison = "No"))

	def createFilterRule(question: String, answer: String, suggestedAnswer: String = "") = (q: HCompQuery) => {
		if (q.question == emptyQ.getInstructions(question, answer) || q.question == emptyQ.getInstructions(question) || q.question == emptyQ.getInstructions(question, suggestedAnswer))
			Some(FreetextAnswer(q.asInstanceOf[FreetextQuery], answer))
		else None
	}

}
