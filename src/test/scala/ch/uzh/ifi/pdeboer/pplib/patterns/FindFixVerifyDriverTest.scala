package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination.ProcessStub
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 30/10/14.
 */
class FindFixVerifyDriverTest {
	val portal = new MockHCompPortal()
	val orderedPatches = List("a", "b", "c").zipWithIndex.map(l => FFVPatch[String](l._1, l._2)).toList

	@Test
	def testFind(): Unit = {
		val driver = new FFVDefaultHCompDriver(orderedPatches, portal, new FFVFindQuestion("findtest"), new FFVFixQuestion("fixtest"))

		val expectedFinds: Set[String] = Set("a", "b")
		portal.createMultipleChoiceFilterRule("findtest", expectedFinds)
		val foundPatches = driver.find(orderedPatches)

		Assert.assertEquals(expectedFinds, foundPatches.map(_.patch).toSet)
	}

	@Test
	def testFix: Unit = {
		val driver = new FFVDefaultHCompDriver(orderedPatches, portal, new FFVFindQuestion("findtest"), new FFVFixQuestion("fixtest"))

		val expectedResult = "b"
		portal.createFreeTextFilterRule("fixtest", expectedResult)
		val fixResult = driver.fix(orderedPatches(0))

		Assert.assertEquals(expectedResult, fixResult.patch)
	}

	@Test
	def testVerify: Unit = {
		val verifyProcess: VerifyTestProcessStub = new VerifyTestProcessStub(Map.empty[String, AnyRef])
		val driver = new FFVDefaultHCompDriver(orderedPatches, portal, new FFVFindQuestion("findtest"), new FFVFixQuestion("fixtest"), verifyProcess = verifyProcess)

		val res = driver.verify(orderedPatches(0), orderedPatches)
		Assert.assertEquals("a", res.patch)
		Assert.assertTrue(verifyProcess.wasCalled)
	}

	private class VerifyTestProcessStub(params: Map[String, AnyRef]) extends ProcessStub[List[String], String](params) {
		var wasCalled: Boolean = false

		override protected def run(data: List[String]): String = {
			wasCalled = true
			"a"
		}
	}
}
