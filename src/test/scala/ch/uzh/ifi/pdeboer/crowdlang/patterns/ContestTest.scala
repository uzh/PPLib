package ch.uzh.ifi.pdeboer.crowdlang.patterns

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 27/10/14.
 */
class ContestTest {
	@Test
	def testVoteOnTargets(): Unit = {
		val exec = new TestableContestExec(
			new NaiveContestDriver(List("a", "b", "c", "d")), 3, 2)

		Assert.assertEquals("a",
			exec.voteOnTargetsAndReturnWinner(exec.driver.alternatives.map(s => exec.ADFromString(s))).alternative)
	}

	@Test
	def testContestOnlyHalf(): Unit = {
		val exec = new TestableContestExec(
			new NaiveContestDriver(List("a", "b", "c", "d")), 3, 2)

		val winner = exec.winner
		Assert.assertEquals(
			exec.castedVotes.groupBy(_.alternative).maxBy(_._2.length)._2(0).alternative,
			exec.winner)
	}

	@Test
	def testContestAllInOne(): Unit = {
		val exec = new TestableContestExec(
			new NaiveContestDriver(List("a", "b", "c", "d")), 3, 100)

		Assert.assertEquals("a", exec.winner)
	}

	private class TestableContestExec(driver: ContestDriver[String], showsPerElement: Int, maxElementsPerGo: Int) extends ContestExecutor(driver, showsPerElement, maxElementsPerGo) {
		var castedVotes = List.empty[AlternativeDetails]

		override def voteOnTargetsAndReturnWinner(target: List[AlternativeDetails]): AlternativeDetails = {
			val ret = super.voteOnTargetsAndReturnWinner(target)
			castedVotes = ret :: castedVotes
			ret
		}

		def ADFromString(s: String) = AlternativeDetails(s, 0, 0)
	}

	/**
	 * always votes for first element
	 * @param alternatives
	 */
	private class NaiveContestDriver(val alternatives: List[String]) extends ContestDriver[String] {
		override def castSingleVote(options: List[String]): String = options(0)
	}

}
