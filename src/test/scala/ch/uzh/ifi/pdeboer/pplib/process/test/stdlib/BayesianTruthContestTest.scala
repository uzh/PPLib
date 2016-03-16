package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.BayesianTruthContest
import ch.uzh.ifi.pdeboer.pplib.util.{CollectionUtils, LazyLogger}
import org.junit.{Assert, Test}

import scala.collection.mutable
import scala.util.Random
import CollectionUtils._

/**
  * Created by pdeboer on 07/03/16.
  */
class BayesianTruthContestTest extends LazyLogger {
	@Test
	def testTrivialCaseAllInFavor: Unit = {
		createPatches.foreach(p => {
			Assert.assertEquals(p, runTrivialCase(createPatches.map(tp => tp -> (tp == p)).toMap))
		})
	}

	@Test
	def testExpertCountMajority: Unit = {
		val expertAnswerChosenCount = (1 to 1000).mpar.map(i => {
			val patches = createPatches
			val playbook = List(createPlaybookAnswer(patches.head), createPlaybookAnswer(patches.head), createPlaybookAnswer(patches(1), patches.head), createPlaybookAnswer(patches(1), patches.head), createPlaybookAnswer(patches(1), patches.head))
			val contest = createContest(new BTTestPortal(playbook))
			val result = contest.process(patches)
			if (patches(1) == result) 1 else 0
		}).sum
		Assert.assertTrue(expertAnswerChosenCount > 500)
	}

	@Test
	def testExpertCountEqualNonExpert: Unit = {
		val expertAnswerChosenCount = (1 to 1000).mpar.map(i => {
			val patches = createPatches
			val playbook = List(createPlaybookAnswer(patches.head), createPlaybookAnswer(patches.head), createPlaybookAnswer(patches(1), patches.head), createPlaybookAnswer(patches(1), patches.head))
			val contest = createContest(new BTTestPortal(playbook))
			val result = contest.process(patches)
			if (patches(1) == result) 1 else 0
		}).sum
		Assert.assertTrue(expertAnswerChosenCount > 500)
	}

	@Test
	def testExpertCountJustBelowNonExpert: Unit = {
		val expertAnswerChosenCount = (1 to 1000).mpar.map(i => {
			val patches = createPatches
			val playbook = List(createPlaybookAnswer(patches.head), createPlaybookAnswer(patches.head), createPlaybookAnswer(patches.head), createPlaybookAnswer(patches(1), patches.head), createPlaybookAnswer(patches(1), patches.head))
			val contest = createContest(new BTTestPortal(playbook))
			val result = contest.process(patches)
			if (patches(1) == result) 1 else 0
		}).sum
		Assert.assertTrue(expertAnswerChosenCount > 500)
	}

	@Test
	def testAnswerUnclear: Unit = {
		val patches = createPatches
		val targetPercentage: Double = 100d / createPatches.length.toDouble
		val playbook = List(createPlaybookAnswer(patches.head, winnerPercentage = targetPercentage), createPlaybookAnswer(patches.head, winnerPercentage = targetPercentage), createPlaybookAnswer(patches(1)))
		val contest = createContest(new BTTestPortal(playbook))
		val result = contest.process(patches)
		Assert.assertEquals(patches.head, result)
	}

	def createPlaybookAnswer(own: Patch, others: Patch = null, allPatches: List[Patch] = createPatches, winnerPercentage: Double = 90d) = BTWorkerAnswer(own.value, {
		val comparisonTarget = if (others == null) own else others
		val loserPercentage = (100d - winnerPercentage) / (allPatches.size.toDouble - 1d)
		allPatches.map(p => p.value -> (if (p == comparisonTarget) winnerPercentage else loserPercentage)).toMap
	})

	def runTrivialCase(winnerSettings: Map[Patch, Boolean]) = {
		val theWinner = winnerSettings.find(w => w._2).get._1
		val playbook = (1 to 3).map(f => createPlaybookAnswer(theWinner))
		val portal = new BTTestPortal(playbook.toList)

		val contest = createContest(portal)
		val result: Patch = contest.process(winnerSettings.keys.toList)
		result
	}

	def createContest(portal: BTTestPortal) = new BayesianTruthContest(Map(PORTAL_PARAMETER.key -> portal, WORKER_COUNT.key -> portal.workerAnswers.size))

	def createPatches: List[IndexedPatch] = {
		IndexedPatch.from("asdf,qwer,yxcv,zuio", ",")
	}

	private[BayesianTruthContestTest] class BTTestPortal(val workerAnswers: List[BTWorkerAnswer]) extends HCompPortalAdapter {

		val answerPool = mutable.Queue.empty[BTWorkerAnswer] ++ workerAnswers.sortBy(s => Random.nextDouble())

		override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
			val answerToUse = answerPool.synchronized(answerPool.dequeue())

			query match {
				case composite: CompositeQuery => Some(CompositeQueryAnswer(composite, composite.queries.map(q => q -> {
					if (q.question.contains("other")) {
						val probability: String = "" + answerToUse.probabilitiesForOthers.find(kv => q.question.contains(kv._1)).get._2
						Some(FreetextAnswer(q, probability))
					} else {
						val mcQuery: MultipleChoiceQuery = q.asInstanceOf[MultipleChoiceQuery]
						val mcQueryOptions: Map[String, Boolean] = mcQuery.options.map(o => o -> (answerToUse.ownAnswer == o)).toMap
						Some(MultipleChoiceAnswer(mcQuery, mcQueryOptions))
					}
				}).toMap))
				case _ => ???
			}
		}

		override def cancelQuery(query: HCompQuery): Unit = ???

		override def getDefaultPortalKey: String = super.getDefaultPortalKey + Random.nextDouble()
	}

	private[BayesianTruthContestTest] case class BTWorkerAnswer(ownAnswer: String, probabilitiesForOthers: Map[String, Double])

}