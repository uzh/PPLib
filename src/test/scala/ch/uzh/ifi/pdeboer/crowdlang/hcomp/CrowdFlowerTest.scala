package ch.uzh.ifi.pdeboer.crowdlang.hcomp

import ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower.CrowdFlowerPortalAdapter
import org.junit.{Assert, Test}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


/**
 * Created by pdeboer on 10/10/14.
 */
class CrowdFlowerTest {
	@Test
	def testFreeText() {
		HComp.crowdFlower = new CrowdFlowerPortalAdapter("PatrickTest", "s2xE6ApWLDRobTg5yj8a")
		val query = HComp.crowdFlower.sendQuery(FreetextQuery("wie heisst du?"))

		Await.result(query, 1 day)
		query onSuccess {
			case answer => println(answer.asInstanceOf[FreetextAnswer].answer)
		}
		Assert.assertTrue(true)
	}


}
