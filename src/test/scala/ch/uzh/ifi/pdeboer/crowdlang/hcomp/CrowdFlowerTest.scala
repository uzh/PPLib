package ch.uzh.ifi.pdeboer.crowdlang.hcomp

import ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower.CrowdFlowerPortalAdapter
import org.junit.{Assert, Test}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


/**
 * Created by pdeboer on 10/10/14.
 * These tests require manual intervention on the CrowdFlower online platform as they are executed in a sandbox.
 * Therefore the Tests are commented out by default for the TestSuite to run thru
 */
class CrowdFlowerTest {
	@Test
	def testFreeText() {
		HComp.addPortal(new CrowdFlowerPortalAdapter("PatrickTest", "s2xE6ApWLDRobTg5yj8a", sandbox = true))
		val query = HComp.crowdFlower.sendQuery(FreetextQuery("wie heisst du?"))

		Await.result(query, 1 day)
		query onSuccess {
			case answer => println(answer.get.asInstanceOf[FreetextAnswer].answer)
		}
		Assert.assertTrue(true)
	}


	//@Test
	def testFreeTextAwait(): Unit = {
		HComp.addPortal(new CrowdFlowerPortalAdapter("PatrickTest", "s2xE6ApWLDRobTg5yj8a", sandbox = true))
		val query = HComp.crowdFlower.sendQueryAndAwaitResult(FreetextQuery("wie heisst du?")).asInstanceOf[FreetextAnswer]
		Assert.assertNotNull(query.answer)
	}
}
