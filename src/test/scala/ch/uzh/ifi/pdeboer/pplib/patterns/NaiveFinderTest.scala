package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTuple, HCompPortalAdapter, MockHCompPortal}
import ch.uzh.ifi.pdeboer.pplib.process.entities.{StringPatch, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.{NoProcessMemoizer, ProcessMemoizer}
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 05/12/14.
 */
class NaiveFinderTest {
	@Test
	def testPartitioning: Unit = {
		val data = List("a", "b", "c").map(p => new StringPatch(p))
		val finder = new NaiveFinderPublic(data, HCompInstructionsWithTuple(""), "title", 2,
			shuffle = false, null, maxItemsPerFind = 2)

		Assert.assertEquals(List(data.take(2), data.take(2), data.takeRight(1), data.takeRight(1)), finder.iterations)
	}

	@Test
	def testSelection: Unit = {
		val data = List("a", "b", "c").map(p => new StringPatch(p))
		val portal: MockHCompPortal = new MockHCompPortal
		portal.createMultipleChoiceFilterRule("nom", Set("b"))

		val finder = new NaiveFinderPublic(data, HCompInstructionsWithTuple("nom"), "title", 2,
			shuffle = false, portal, maxItemsPerFind = 3)

		Assert.assertEquals(1, finder.selectionsPerPatch.size)
		Assert.assertEquals(2, finder.selectionsPerPatch(new StringPatch("b")))
		Assert.assertEquals(Map(new StringPatch("a") -> 0, new StringPatch("b") -> 2, new StringPatch("c") -> 0), finder.result)
	}

	private class NaiveFinderPublic(data: List[Patch], question: HCompInstructionsWithTuple, title: String, findersPerItem: Int, shuffle: Boolean, portal: HCompPortalAdapter, maxItemsPerFind: Int = 5, memoizer: ProcessMemoizer = new NoProcessMemoizer())
		extends NaiveFinder(data, question, title, findersPerItem, shuffle, portal, maxItemsPerFind, memoizer) {
		def iterations: List[List[Patch]] = selectionIterations.map(_.map(_.patch))
	}

}
