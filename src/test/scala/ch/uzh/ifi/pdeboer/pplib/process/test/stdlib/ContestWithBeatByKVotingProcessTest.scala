package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal.RandomHCompPortal
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess
import org.junit.{Assert, Test}

/**
  * Created by pdeboer on 03/12/14.
  */
class ContestWithBeatByKVotingProcessTest {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess._

	@Test
	def testBestAndSecondBest: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess()
		p.setVotes(Map("a" -> 1, "b" -> 2))

		Assert.assertEquals((("b", 2), ("a", 1)), p.bestAndSecondBest)
	}

	@Test
	def testDelta: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess()
		p.setVotes(Map("a" -> 1, "b" -> 2))

		Assert.assertEquals(1, p.delta)

		p.setVotes(Map("a" -> 2))
		Assert.assertEquals(0, p.delta)
	}

	@Test
	def testKDiff: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess(Map(K.key -> 3))
		p.setVotes(Map("a" -> 1, "b" -> 2))

		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 1, "b" -> 3))
		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 1, "b" -> 4))
		Assert.assertFalse(p.shouldStartAnotherIteration)
	}

	@Test
	def testMaxIterationsExceeded: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess(Map(K.key -> 3, DefaultParameters.MAX_ITERATIONS.key -> 9))
		p.setVotes(Map("a" -> 4, "b" -> 2))
		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 5, "b" -> 2))
		Assert.assertFalse(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 5, "b" -> 5))
		Assert.assertFalse(p.shouldStartAnotherIteration)
	}

	@Test
	def testRuntimeProblem: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess(Map(K.key -> 4, DefaultParameters.MAX_ITERATIONS.key -> 10))
		p.setVotes(Map("a" -> 5, "b" -> 2))

		Assert.assertTrue(p.shouldStartAnotherIteration)
	}

	@Test
	def testMaxIterationsWithUncountedVotes: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess(Map(K.key -> 3, DefaultParameters.MAX_ITERATIONS.key -> 10))
		p.setVotes(Map("a" -> 3, "b" -> 3))
		p.setUncountedVotes(1)
		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setUncountedVotes(2)
		Assert.assertFalse(p.shouldStartAnotherIteration)
	}

	@Test
	def testEndResult: Unit = {
		val patches = IndexedPatch.from(List("a", "b"))
		val p = new MiniContestWithBeatByKVotingProcess(Map(K.key -> 3, DefaultParameters.MAX_ITERATIONS.key -> 9))
		p.setVotes(Map("a" -> 5, "b" -> 2))
		Assert.assertEquals(patches.head, p.getEndResult(patches))

		//p.setParam(RETURN_LEADER_IF_MAX_ITERATIONS_REACHED.key, true)  <-- default is true.
		p.setVotes(Map("a" -> 3, "b" -> 5))
		Assert.assertEquals(patches(1), p.getEndResult(patches))

		p.setParam(RETURN_LEADER_IF_MAX_ITERATIONS_REACHED.key, false)
		Assert.assertNull(p.getEndResult(patches))

		p.setVotes(Map("a" -> 5, "b" -> 5))
		Assert.assertNull(p.getEndResult(patches))

		p.setParam(RETURN_LEADER_IF_MAX_ITERATIONS_REACHED.key, true)
		Assert.assertNotNull(p.getEndResult(patches))
	}

	@Test
	def testMemoizer: Unit = {
		import DefaultParameters._
		val mem = new InMemoryProcessMemoizer("bbkInMem")
		val portal = new RandomHCompPortal("")
		val bbkParamMap = Map(MEMOIZER_NAME.key -> Some("mem"), PORTAL_PARAMETER.key -> portal, K.key -> 4)
		val data: List[IndexedPatch] = IndexedPatch.from("1,2,3,4,5,6,7,8,9", ",")
		val bbk = new MiniContestWithBeatByKVotingProcess(bbkParamMap, mem)
		val winner = bbk.process(data)

		val bbk2 = new MiniContestWithBeatByKVotingProcess(bbkParamMap, mem)
		val winner2 = bbk2.process(data)

		Assert.assertEquals(winner, winner2)
		Assert.assertEquals(bbk.getVotes, bbk2.getVotes)
	}

	private class MiniContestWithBeatByKVotingProcess(params: Map[String, Any] = Map.empty[String, Any], mem: ProcessMemoizer = null) extends ContestWithBeatByKVotingProcess(params) {

		import DefaultParameters._

		def getVotes = votes

		def setVotes(v: Map[String, Int]): Unit = {
			votes ++= v
		}

		def setUncountedVotes(uncounted: Int): Unit = {
			this.uncountedVotes = uncounted
		}

		override def getProcessMemoizer(identity: String): Option[ProcessMemoizer] = if (MEMOIZER_NAME.get.isDefined) Some(mem) else None
	}

}
