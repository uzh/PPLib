package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTupleStringified, HCompPortalAdapter, MockHCompPortal}
import ch.uzh.ifi.pdeboer.pplib.process.entities.{NoProcessMemoizer, ProcessMemoizer, Patch}
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 05/12/14.
 */
class ContestWithMultipleEqualWinnersTest {
	@Test
	def testPartitioning: Unit = {
		val data = List("a", "b", "c").map(p => new Patch(p))
		val finder = new ContestWithMultipleEqualWinnersPublic(data, HCompInstructionsWithTupleStringified(""), "title", 2,
			shuffle = false, null, maxItemsPerFind = 2)

		Assert.assertEquals(List(data.take(2), data.take(2), data.takeRight(1), data.takeRight(1)), finder.iterations)
	}

	@Test
	def testSelection: Unit = {
		val data = List("a", "b", "c").map(p => new Patch(p))
		val portal: MockHCompPortal = new MockHCompPortal
		portal.createMultipleChoiceFilterRule("nom", Set("b"))

		val finder = new ContestWithMultipleEqualWinnersPublic(data, HCompInstructionsWithTupleStringified("nom"), "title", 2,
			shuffle = false, portal, maxItemsPerFind = 3)

		Assert.assertEquals(1, finder.selectionsPerPatch.size)
		Assert.assertEquals(2, finder.selectionsPerPatch(new Patch("b")))
		Assert.assertEquals(Map(new Patch("a") -> 0, new Patch("b") -> 2, new Patch("c") -> 0), finder.result)
	}

	private class ContestWithMultipleEqualWinnersPublic(data: List[Patch], question: HCompInstructionsWithTupleStringified, title: String, findersPerItem: Int, shuffle: Boolean, portal: HCompPortalAdapter, maxItemsPerFind: Int = 5, memoizer: ProcessMemoizer = new NoProcessMemoizer())
		extends ContestWithMultipleEqualWinners(data, question, title, findersPerItem, shuffle, portal, maxItemsPerFind, memoizer) {
		def iterations: List[List[Patch]] = selectionIterations.map(_.map(_.patch))
	}
}
