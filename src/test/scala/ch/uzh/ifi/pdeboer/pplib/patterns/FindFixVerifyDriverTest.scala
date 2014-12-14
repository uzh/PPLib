package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.VerifyTestProcessStub
import ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam
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
		val verifyProcess = new PassableProcessParam[List[String], String](classOf[VerifyTestProcessStub])
		val driver = new FFVDefaultHCompDriver(orderedPatches, portal, new FFVFindQuestion("findtest"), new FFVFixQuestion("fixtest"), verifyProcessParam = verifyProcess)

		val res = driver.verify(orderedPatches(0), orderedPatches)
		Assert.assertEquals("a", res.patch)
		Assert.assertEquals(1, verifyProcess.createdProcesses.length)
		Assert.assertTrue(verifyProcess.createdProcesses(0).asInstanceOf[VerifyTestProcessStub].wasCalled)
	}
}

