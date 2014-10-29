package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 29/10/14.
 */
class DualPathWayDefaultHCompDriverComparisonTest {
	val portal = new MockHCompPortal()
	val emptyQ: HCompInstructionsWithData = HCompInstructionsWithData("")
	val data: List[String] = List("a", "b", "c", "d")

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

		val pathway = driver.indexMap.map(i => DPChunk(i._1, i._2, i._2)).toList

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

		val pathway = driver.indexMap.map(i => DPChunk(i._1, i._2, i._2)).toList

		val ret = driver.comparePathwaysAndDecideWhetherToAdvance(
			pathway, pathway
		)

		Assert.assertFalse(ret)
	}

	def newDriver(text: String) = new DualPathWayDefaultHCompDriver(data, portal, emptyQ, emptyQ, "", new DefaultComparisonInstructionsConfig(text))
}

class DualPathWayDefaultHCompDriverProcessingTest {
	val portal = new MockHCompPortal()
	portal.filters = List(
		createFilterRule("a", "b"), createFilterRule("b", "c"), createFilterRule("c", "d"),
		createFilterRule("d", "e")
	)
	val data: List[String] = List("a", "b", "c", "d")
	val emptyQ: HCompInstructionsWithData = HCompInstructionsWithData("")

	@Test
	def testProcessingUnitAllCorrectInclNewItem(): Unit = {
		val driver = newDriver
		val ret = driver.processChunksAndPossiblyAddNew(
			List(
				DPChunk(1, "b", "c"), DPChunk(0, "a", "b")),
			Some(2)
		)

		Assert.assertEquals(List("b", "c", "d").toSet, ret.map(_.answer).toSet)
	}

	@Test
	def testProcessingUnitErrorCorrectionInclNewItem(): Unit = {
		val driver = newDriver

		val retFixedError = driver.processChunksAndPossiblyAddNew(
			List(
				DPChunk(1, "b", "WRONG"), DPChunk(0, "a", "WRONG2")),
			Some(2)
		)
		Assert.assertEquals(List("b", "c", "d").toSet, retFixedError.map(_.answer).toSet)
	}

	@Test
	def testProcessingUnitAllCorrectExclNewItem(): Unit = {
		val driver = newDriver
		val ret = driver.processChunksAndPossiblyAddNew(
			List(
				DPChunk(1, "b", "c"), DPChunk(0, "a", "b")))

		Assert.assertEquals(List("b", "c").toSet, ret.map(_.answer).toSet)
	}

	@Test
	def testProcessingUnitErrorCorrectionExclNewItem(): Unit = {
		val driver = newDriver

		val retFixedError = driver.processChunksAndPossiblyAddNew(
			List(
				DPChunk(1, "b", "WRONG"), DPChunk(0, "a", "WRONG2")))
		Assert.assertEquals(List("b", "c").toSet, retFixedError.map(_.answer).toSet)
	}


	def newDriver = new DualPathWayDefaultHCompDriver(data, portal, emptyQ, emptyQ, "", new DefaultComparisonInstructionsConfig(""))

	def createFilterRule(question: String, answer: String) = (q: HCompQuery) => {
		if (q.question.equals(HCompInstructionsWithData("").getInstructions(question)))
			Some(FreetextAnswer(q.asInstanceOf[FreetextQuery], answer))
		else None
	}

}
