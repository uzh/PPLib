package ch.uzh.ifi.pdeboer.pplib.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter
import com.typesafe.config.ConfigFactory
import org.junit.{Test, Assert}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


/**
 * Created by pdeboer on 10/10/14.
 * These tests require manual intervention on the CrowdFlower online platform as they are executed in a sandbox.
 * Therefore the Tests are commented out by default for the TestSuite to run thru
 */
class CrowdFlowerTest {
	val config = ConfigFactory.load()
	val apiKey = config.getString("hcomp.crowdflower.apikey")

	//@Test
	def testFreeText() {
		HComp.addPortal(new CrowdFlowerPortalAdapter("PatrickTest", sandbox = true))
		val query = HComp.crowdFlower.sendQuery(FreetextQuery("wie heisst du?"))

		Await.result(query, 1 day)
		query onSuccess {
			case answer => println(answer.get.asInstanceOf[FreetextAnswer].answer)
		}
		Assert.assertTrue(true)
	}

	//@Test
	def testFreeTextFixTask() {
		HComp.addPortal(new CrowdFlowerPortalAdapter("PatrickTest", sandbox = true))
		val data: String = """The particles should be able to dock with cells,
							 | proteins or other molecules within a human body, explained Andrew Conrad, head of the
							 | team for life sciences laboratory Google X at the conference WSJD Live the
							 | Wall Street Journal.""".stripMargin
		val res = HComp.crowdFlower.sendQueryAndAwaitResult(
			FreetextQuery(HCompInstructionsWithTuple("Other crowd workers have agreed on this sentence being erroneous. Please fix it")
				.getInstructions(data))
		).get.asInstanceOf[FreetextAnswer]

		Assert.assertNotNull(res.answer)
	}

	//@Test
	def testMultipleChoice: Unit = {
		HComp.addPortal(new CrowdFlowerPortalAdapter("PatrickTest", sandbox = true))
		val query = HComp.crowdFlower.sendQuery(MultipleChoiceQuery("what's 1+1", List("1", "2", "3"), 2))

		Await.result(query, 1 day)
		query onSuccess {
			case answer => println(answer.get.asInstanceOf[MultipleChoiceAnswer].answer)
		}
		Assert.assertTrue(true)
	}

	//@Test
	def testCompositeChoice: Unit = {
		HComp.addPortal(new CrowdFlowerPortalAdapter("PatrickTest", sandbox = true))
		val query = HComp.crowdFlower.sendQuery(
			CompositeQuery(List(
				FreetextQuery("what's your name?"),
				MultipleChoiceQuery("what's 1+1", List("1", "2", "3"), 2)
			), "please answer the following questions")
		)

		Await.result(query, 1 day)
		query onSuccess {
			case answer => println(answer.getOrElse("nope").toString)
		}
		Assert.assertTrue(true)
	}
}
