package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{MockHCompPortal, StringQuestionRenderer}
import ch.uzh.ifi.pdeboer.pplib.process.entities.{DefaultParameters, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithStatisticalReductionProcess
import ch.uzh.ifi.pdeboer.pplib.util.MonteCarlo
import org.junit.{Assert, Test}

import scala.util.Random

/**
 * Created by pdeboer on 03/11/14.
 */
class ContestWithStatisticalReductionProcessTest {
	@Test
	def TestConfidence90Correct(): Unit = {
		// if 11 responses have been received for a question with 5 choices, only 6 must agree to achieve a 90% confidence
		val data = List("1", "2", "3", "4", "5").map(n => new Patch(n))
		val dataMasterPlanWithVotes = (data zip List(1, 0, 1, 1, 2)).toMap

		val subject = new ContestWithStatisticalReductionProcessTestMasterPlan(
			dataMasterPlanWithVotes,
			Map(
				ContestWithStatisticalReductionProcess.CONFIDENCE_PARAMETER.key -> 0.9,
				DefaultParameters.PORTAL_PARAMETER.key -> new MockHCompPortal(),
				DefaultParameters.SHUFFLE_CHOICES.key -> false
			))

		Assert.assertEquals(new Patch("2"), subject.process(data))

		val minVotesForAgreement = MonteCarlo.minAgreementRequired(data.size, subject.votesCastPub.values.sum, 0.9, 100000).get
		Assert.assertEquals(minVotesForAgreement, subject.votesCastPub.apply("2"))

		Assert.assertEquals(6, subject.votesCastPub.apply("2"))
		Assert.assertEquals(11, subject.votesCastPub.values.sum)
	}

	@Test
	def TestConfidence95Correct(): Unit = {
		// if 11 responses have been received for a question with 5 choices, only 7 must agree to achieve a 95% confidence
		val data = List("1", "2", "3", "4", "5").map(i => new Patch(i))
		val dataMasterPlanWithVotes = (data zip List(1, 0, 1, 1, 2)).toMap

		val subject = new ContestWithStatisticalReductionProcessTestMasterPlan(
			dataMasterPlanWithVotes,
			Map(
				DefaultParameters.INSTRUCTIONS.key -> StringQuestionRenderer(""),
				ContestWithStatisticalReductionProcess.CONFIDENCE_PARAMETER.key -> 0.95,
				DefaultParameters.PORTAL_PARAMETER.key -> new MockHCompPortal(),
				DefaultParameters.SHUFFLE_CHOICES.key -> false
			))

		Assert.assertEquals(new Patch("2"), subject.process(data))

		val minVotesForAgreement = MonteCarlo.minAgreementRequired(data.size, subject.votesCastPub.values.sum, 0.95, 100000).get
		Assert.assertEquals(minVotesForAgreement, subject.votesCastPub.apply("2"))

		Assert.assertEquals(7, subject.votesCastPub.apply("2"))
		Assert.assertEquals(12, subject.votesCastPub.values.sum)
	}

	private class ContestWithStatisticalReductionProcessTestMasterPlan(val masterPlan: Map[Patch, Int], params: Map[String, Any]) extends ContestWithStatisticalReductionProcess(params) {
		var votesToCast = scala.collection.mutable.HashMap.empty[Patch, Int]
		masterPlan.foreach(e => votesToCast += (e._1 -> e._2))

		def votesCastPub = votesCast

		override def castVote(alternatives: List[String], iteration: Int): String = {
			val nonWinningCandidates = votesToCast.filter(k => k._2 > 0).keys
				.map(l => (l, Random.nextDouble())).toList.sortBy(_._2)
			//cast random vote out of master plan
			val target = if (nonWinningCandidates.size > 0) nonWinningCandidates(0)._1 else new Patch("2")
			println(s"voted $target")
			votesToCast += target -> (votesToCast.get(target).get - 1)
			target.value
		}
	}

}
