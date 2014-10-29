package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 29/10/14.
 */
class DefaultDualPathWayHCompDriverTest {
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

	def newDriver = new DefaultDualPathWayHCompDriver(data, portal, emptyQ, emptyQ, "", new DefaultComparisonInstructionsConfig(""))

	def createFilterRule(question: String, answer: String) = (q: HCompQuery) => {
		if (q.question.equals(HCompInstructionsWithData("").getInstructions(question)))
			Some(FreetextAnswer(q.asInstanceOf[FreetextQuery], answer))
		else None
	}

}
