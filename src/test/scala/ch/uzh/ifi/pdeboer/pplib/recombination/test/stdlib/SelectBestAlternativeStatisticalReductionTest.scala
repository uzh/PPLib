package ch.uzh.ifi.pdeboer.pplib.recombination.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{MockHCompPortal, HCompInstructionsWithTuple}
import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.SelectBestAlternativeStatisticalReduction
import ch.uzh.ifi.pdeboer.pplib.recombination.{ProcessStubWithHCompPortalAccess, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.MonteCarlo
import org.junit.{Assert, Test}

import scala.util.Random

/**
 * Created by pdeboer on 03/11/14.
 */
class SelectBestAlternativeStatisticalReductionTest {
	@Test
	def TestConfidence90Correct(): Unit = {
		// if 11 responses have been received for a question with 5 choices, only 6 must agree to achieve a 90% confidence
		val data: List[String] = List("1", "2", "3", "4", "5")
		val dataMasterPlanWithVotes = (data zip List(1, 0, 1, 1, 2)).toMap

		val subject = new SelectBestAlternativeStatisticalReductionTestMasterPlan(
			dataMasterPlanWithVotes,
			Map(
				SelectBestAlternativeStatisticalReduction.INSTRUCTIONS_PARAMETER.key -> HCompInstructionsWithTuple(""),
				SelectBestAlternativeStatisticalReduction.CONFIDENCE_PARAMETER.key -> 0.9,
				ProcessStubWithHCompPortalAccess.PORTAL_PARAMETER.key -> new MockHCompPortal(),
				SelectBestAlternativeStatisticalReduction.SHUFFLE_CHOICES.key -> false

			))

		Assert.assertEquals("2", subject.process(data))

		val minVotesForAgreement = MonteCarlo.minAgreementRequired(data.size, subject.votesCastPub.values.sum, 0.9, 100000).get
		Assert.assertEquals(minVotesForAgreement, subject.votesCastPub.apply("2"))

		Assert.assertEquals(6, subject.votesCastPub.apply("2"))
		Assert.assertEquals(11, subject.votesCastPub.values.sum)
	}

	@Test
	def TestConfidence95Correct(): Unit = {
		// if 11 responses have been received for a question with 5 choices, only 7 must agree to achieve a 95% confidence
		val data: List[String] = List("1", "2", "3", "4", "5")
		val dataMasterPlanWithVotes = (data zip List(1, 0, 1, 1, 2)).toMap

		val subject = new SelectBestAlternativeStatisticalReductionTestMasterPlan(
			dataMasterPlanWithVotes,
			Map(
				SelectBestAlternativeStatisticalReduction.INSTRUCTIONS_PARAMETER.key -> HCompInstructionsWithTuple(""),
				SelectBestAlternativeStatisticalReduction.CONFIDENCE_PARAMETER.key -> 0.95,
				ProcessStubWithHCompPortalAccess.PORTAL_PARAMETER.key -> new MockHCompPortal(),
				SelectBestAlternativeStatisticalReduction.SHUFFLE_CHOICES.key -> false
			))

		Assert.assertEquals("2", subject.process(data))

		val minVotesForAgreement = MonteCarlo.minAgreementRequired(data.size, subject.votesCastPub.values.sum, 0.95, 100000).get
		Assert.assertEquals(minVotesForAgreement, subject.votesCastPub.apply("2"))

		Assert.assertEquals(7, subject.votesCastPub.apply("2"))
		Assert.assertEquals(12, subject.votesCastPub.values.sum)
	}

	private class SelectBestAlternativeStatisticalReductionTestMasterPlan(val masterPlan: Map[String, Int], params: Map[String, Any]) extends SelectBestAlternativeStatisticalReduction(params) {
		var votesToCast = scala.collection.mutable.HashMap.empty[String, Int]
		masterPlan.foreach(e => votesToCast += (e._1 -> e._2))

		def votesCastPub = votesCast

		override def castVote(alternatives: List[String]): String = {
			val nonWinningCandidates: List[(String, Double)] = votesToCast.filter(k => k._2 > 0).keys
				.map(l => (l, Random.nextDouble())).toList.sortBy(_._2)
			//cast random vote out of master plan
			val target = if (nonWinningCandidates.size > 0) nonWinningCandidates(0)._1 else "2"
			println(s"voted $target")
			votesToCast += target -> (votesToCast.get(target).get - 1)
			target
		}
	}

}
