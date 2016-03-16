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
		val p = new MiniContestWithBeatByKVotingProcess(patches = IndexedPatch.from("a,b", ","))
		p.setVotes(Map("a" -> 1, "b" -> 2))

		Assert.assertEquals("b", p.bestAndSecondBest._1._1.value)
		Assert.assertEquals("a", p.bestAndSecondBest._2._1.value)

		Assert.assertEquals(2d, p.bestAndSecondBest._1._2, 0.1d)
		Assert.assertEquals(1d, p.bestAndSecondBest._2._2, 0.1d)
	}

	@Test
	def testDelta: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess(patches = IndexedPatch.from("a,b", ","))
		p.setVotes(Map("a" -> 1, "b" -> 2))

		Assert.assertEquals(1d, p.delta, 0.1d)

		p.setVotes(Map("a" -> 2))
		Assert.assertEquals(0d, p.delta, 0.1d)
	}

	@Test
	def testKDiff: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess(Map(K.key -> 3), patches = IndexedPatch.from("a,b", ","))
		p.setVotes(Map("a" -> 1, "b" -> 2))

		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 1, "b" -> 3))
		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 1, "b" -> 4))
		Assert.assertFalse(p.shouldStartAnotherIteration)
	}

	@Test
	def testMaxIterationsExceeded: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess(Map(K.key -> 3, DefaultParameters.MAX_ITERATIONS.key -> 9), patches = IndexedPatch.from("a,b", ","))
		p.setVotes(Map("a" -> 4, "b" -> 2))
		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 5, "b" -> 2))
		Assert.assertFalse(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 5, "b" -> 5))
		Assert.assertFalse(p.shouldStartAnotherIteration)
	}

	@Test
	def testRuntimeProblem: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess(Map(K.key -> 4, DefaultParameters.MAX_ITERATIONS.key -> 10), patches = IndexedPatch.from("a,b", ","))
		p.setVotes(Map("a" -> 5, "b" -> 2))

		Assert.assertTrue(p.shouldStartAnotherIteration)
	}

	@Test
	def testMaxIterationsWithUncountedVotes: Unit = {
		val p = new MiniContestWithBeatByKVotingProcess(Map(K.key -> 3, DefaultParameters.MAX_ITERATIONS.key -> 10), patches = IndexedPatch.from("a,b", ","))
		p.setVotes(Map("a" -> 3, "b" -> 3))
		p.setUncountedVotes(1)
		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setUncountedVotes(2)
		Assert.assertFalse(p.shouldStartAnotherIteration)
	}

	@Test
	def testEndResult: Unit = {
		val patches = IndexedPatch.from(List("a", "b"))
		val p = new MiniContestWithBeatByKVotingProcess(Map(K.key -> 3, DefaultParameters.MAX_ITERATIONS.key -> 9), patches = IndexedPatch.from("a,b", ","))
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

	//@Test
	def testMemoizer: Unit = {
		import DefaultParameters._
		val mem = new InMemoryProcessMemoizer("bbkInMem")
		val portal = new RandomHCompPortal("")
		val bbkParamMap = Map(MEMOIZER_NAME.key -> Some("mem"), PORTAL_PARAMETER.key -> portal, K.key -> 4)
		val data: List[IndexedPatch] = IndexedPatch.from("1,2,3,4,5,6,7,8,9", ",")
		val bbk = new MiniContestWithBeatByKVotingProcess(bbkParamMap, mem, data)
		val winner = bbk.process(data)

		val bbk2 = new MiniContestWithBeatByKVotingProcess(bbkParamMap, mem, data)
		val winner2 = bbk2.process(data)

		Assert.assertEquals(winner, winner2)
		Assert.assertEquals(bbk.getVotes, bbk2.getVotes)
	}

	private class MiniContestWithBeatByKVotingProcess(params: Map[String, Any] = Map.empty[String, Any], mem: ProcessMemoizer = null, patches: List[Patch]) extends ContestWithBeatByKVotingProcess(params) {

		import DefaultParameters._

		def getVotes = votes

		def setVotes(v: Map[String, Int]): Unit = {
			votes.clear()
			v.foreach(mv => {
				val patch: Patch = patches.find(_.value == mv._1).get
				(1 to mv._2).foreach(n => votes.addVote(patch, null))
			})
		}

		def setUncountedVotes(uncounted: Int): Unit = {
			this.uncountedVotes = uncounted
		}

		override def getProcessMemoizer(identity: String): Option[ProcessMemoizer] = if (MEMOIZER_NAME.get.isDefined) Some(mem) else None
	}

}
