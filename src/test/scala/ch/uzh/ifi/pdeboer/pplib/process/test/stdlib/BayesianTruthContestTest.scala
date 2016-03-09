package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.BayesianTruthContest
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.junit.{Assert, Test}

import scala.collection.mutable
import scala.util.Random

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

	def runTrivialCase(winnerSettings: Map[Patch, Boolean]) = {
		val othersSelections = winnerSettings.map(p => p._1.toString -> (if (p._2) 100.0d else 0d)).toMap
		val theWinner = winnerSettings.find(w => w._2).get._1.value
		val portal = new BTTestPortal(List(BTWorkerAnswer(theWinner, othersSelections), BTWorkerAnswer(theWinner, othersSelections), BTWorkerAnswer(theWinner, othersSelections)))

		val contest = new BayesianTruthContest(Map(PORTAL_PARAMETER.key -> portal, WORKER_COUNT.key -> 3))
		val result: Patch = contest.process(winnerSettings.keys.toList)
		result
	}

	def createPatches: List[IndexedPatch] = {
		IndexedPatch.from("asdf,qwer,yxcv,zuio", ",")
	}

	private[BayesianTruthContestTest] class BTTestPortal(workerAnswers: List[BTWorkerAnswer]) extends HCompPortalAdapter {

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
	}

	private[BayesianTruthContestTest] case class BTWorkerAnswer(ownAnswer: String, probabilitiesForOthers: Map[String, Double])

}