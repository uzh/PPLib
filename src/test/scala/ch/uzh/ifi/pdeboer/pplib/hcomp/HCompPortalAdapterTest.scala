package ch.uzh.ifi.pdeboer.pplib.hcomp

import org.junit.{Assert, Before}

import scala.concurrent.duration._

/**
 * Created by pdeboer on 11/12/14.
 */
class HCompPortalAdapterTest {
	var interruptionCounter = 0

	def wait1Sec(maxCounter: Int = 1): (HCompQuery, HCompQueryProperties) => Some[FreetextAnswer] = (q: HCompQuery, p: HCompQueryProperties) => {
		if (interruptionCounter < maxCounter)
			Thread.sleep(1000)
		Some(FreetextAnswer(q.asInstanceOf[FreetextQuery], "worked"))
	}

	@Before def resetCounter: Unit = {
		interruptionCounter = 0
	}

	//@Test
	def testNoCancelReport: Unit = {
		val portal = new MyTestPortalAdapter(wait1Sec())

		val q = new FreetextQuery("question")
		val answer = portal.sendQueryAndAwaitResult(q, HCompQueryProperties(cancelAndRepostAfter = 1 hour), 1 hour)
		Assert.assertEquals("worked", answer.get.is[FreetextAnswer].answer)
		Assert.assertEquals(1, portal.calls.length)
	}

	//@Test
	def testCancelAndRepost: Unit = {
		val portal = new MyTestPortalAdapter(wait1Sec())

		val q = new FreetextQuery("question")
		val answer = portal.sendQueryAndAwaitResult(q, HCompQueryProperties(cancelAndRepostAfter = 600 millis), 1 hour)
		Assert.assertEquals("worked", answer.get.is[FreetextAnswer].answer)
		println(portal.calls.mkString(","))
		Assert.assertEquals(3, portal.calls.length)
	}

	//@Test
	def testQueryTimeout: Unit = {
		val portal = new MyTestPortalAdapter(wait1Sec())

		val q = new FreetextQuery("question")
		val answer = portal.sendQueryAndAwaitResult(q, HCompQueryProperties(cancelAndRepostAfter = 1200 millis), 800 millis)
		Assert.assertEquals(None, answer)
		Assert.assertEquals(1, portal.calls.length)
	}


	//@Test
	def testQueryRepostAndTimeout: Unit = {
		val portal = new MyTestPortalAdapter(wait1Sec(5))

		val q = new FreetextQuery("question")
		val answer = portal.sendQueryAndAwaitResult(q, HCompQueryProperties(cancelAndRepostAfter = 300 millis), 800 millis)
		Assert.assertEquals(None, answer)
		Assert.assertEquals(5, portal.calls.length)
	}

	private class MyTestPortalAdapter(q: (HCompQuery, HCompQueryProperties) => Option[HCompAnswer]) extends HCompPortalAdapter {
		var calls = List.empty[(String, HCompQuery)]

		override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
			calls = ("process", query) :: calls
			q(query, properties)
		}

		override def getDefaultPortalKey: String = getClass.getSimpleName

		override def cancelQuery(query: HCompQuery): Unit = {
			calls = ("cancel", query) :: calls
			interruptionCounter += 1
		}
	}

}
