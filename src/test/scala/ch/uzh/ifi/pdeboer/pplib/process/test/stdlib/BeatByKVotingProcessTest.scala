package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.stdlib.BeatByKVotingProcess
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 03/12/14.
 */
class BeatByKVotingProcessTest {

	import BeatByKVotingProcess._

	@Test
	def testBestAndSecondBest: Unit = {
		val p = new MiniBeatByKVotingProcess()
		p.setVotes(Map("a" -> 1, "b" -> 2))

		Assert.assertEquals((("b", 2), ("a", 1)), p.bestAndSecondBest)
	}

	@Test
	def testDelta: Unit = {
		val p = new MiniBeatByKVotingProcess()
		p.setVotes(Map("a" -> 1, "b" -> 2))

		Assert.assertEquals(1, p.delta)

		p.setVotes(Map("a" -> 2))
		Assert.assertEquals(0, p.delta)
	}

	@Test
	def testKDiff: Unit = {
		val p = new MiniBeatByKVotingProcess(Map(K.key -> 3))
		p.setVotes(Map("a" -> 1, "b" -> 2))

		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 1, "b" -> 3))
		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 1, "b" -> 4))
		Assert.assertFalse(p.shouldStartAnotherIteration)
	}

	@Test
	def testMaxIterationsExceeded: Unit = {
		val p = new MiniBeatByKVotingProcess(Map(K.key -> 3, MAX_VOTES.key -> 10))
		p.setVotes(Map("a" -> 4, "b" -> 2))
		Assert.assertTrue(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 5, "b" -> 2))
		Assert.assertFalse(p.shouldStartAnotherIteration)

		p.setVotes(Map("a" -> 6, "b" -> 5))
		Assert.assertFalse(p.shouldStartAnotherIteration)
	}

	private class MiniBeatByKVotingProcess(params: Map[String, Any] = Map.empty[String, Any]) extends BeatByKVotingProcess(params) {
		def setVotes(v: Map[String, Int]): Unit = {
			votes = votes ++ v
		}
	}

}
