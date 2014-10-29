package ch.uzh.ifi.pdeboer.crowdlang.patterns

import ch.uzh.ifi.pdeboer.crowdlang.hcomp._
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 29/10/14.
 */
class DefaultDualPathWayHCompDriverTest {
	@Test
	def testComparisonLogic(): Unit = {
		val portal = new MockHCompPortal()
		portal.filters = List(
			createFilterRule("a", "b"), createFilterRule("b", "c"), createFilterRule("c", "d"),
			createFilterRule("d", "e")
		)

		val emptyQ: HCompInstructionsWithData = HCompInstructionsWithData("")
		val driver = new DefaultDualPathWayHCompDriver(List("a", "b", "c", "d"), portal, emptyQ, emptyQ, "", new DefaultComparisonInstructionsConfig(""))

		val ret = driver.processChunksAndPossiblyAddNew(
			List(
				DPChunk(0, "a", "b"),
				DPChunk(1, "b", "c")),
			Some(2)
		)

		Assert.assertEquals(List("b", "c", "d").toSet, ret.map(_.answer).toSet)
	}

	def createFilterRule(question: String, answer: String) = (q: HCompQuery) => {
		if (q.question.equals(HCompInstructionsWithData("").getInstructions(question)))
			Some(FreetextAnswer(q.asInstanceOf[FreetextQuery], answer))
		else None
	}

}
