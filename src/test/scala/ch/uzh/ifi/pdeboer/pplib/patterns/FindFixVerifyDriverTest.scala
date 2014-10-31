package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 30/10/14.
 */
class FindFixVerifyDriverTest {
	val portal = new MockHCompPortal()
	val orderedPatches = List("a", "b", "c").zipWithIndex.map(l => FFVPatch[String](l._1, l._2)).toList

	@Test
	def testFind(): Unit = {
		val driver = new FFVDefaultHCompDriver(orderedPatches, portal, "findtest",
			new HCompInstructionsWithData("fixtest"))

		val expectedFinds: Set[String] = Set("a", "b")
		portal.createMultipleChoiceFilterRule("findtest", expectedFinds)
		val foundPatches = driver.find(orderedPatches)

		Assert.assertEquals(expectedFinds, foundPatches.map(_.patch).toSet)
	}

	@Test
	def testFix: Unit = {
		val driver = new FFVDefaultHCompDriver(orderedPatches, portal, "findtest",
			new HCompInstructionsWithData("fixtest"))

		val expectedResult = "b"
		portal.createFreeTextFilterRule("fixtest", expectedResult)
		val fixResult = driver.fix(orderedPatches(0))

		Assert.assertEquals(expectedResult, fixResult.patch)
	}
}
