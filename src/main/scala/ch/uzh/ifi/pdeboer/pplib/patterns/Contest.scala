package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompPortalAdapter, MultipleChoiceAnswer, MultipleChoiceQuery}

import scala.concurrent.duration._
import scala.util.Random

/**
 * Created by pdeboer on 24/10/14.
 */
class ContestExecutor[T](val driver: ContestDriver[T], val showsPerElement: Int = 3, val maxElementsPerGo: Int = 100) {
	lazy val winner: T = {
		do {
			step()
		} while (alternatives.minBy(_._2.numberOfShows)._2.numberOfShows < showsPerElement)

		val candidates = winnerCandidates
		if (candidates.size == 1)
			candidates(0)._2.alternative
		else
			voteOnTargetsAndReturnWinner(candidates.map(_._2)).alternative
	}
	protected val alternatives = driver.alternatives.zipWithIndex.map(a => a._2 -> new AlternativeDetails(a._1)).toMap

	protected def winnerCandidates = {
		val maxSelects = alternatives.maxBy(_._2.numberOfSelects)._2.numberOfSelects
		alternatives.filter(_._2.numberOfSelects == maxSelects).toList
	}

	protected def step(): Unit = {
		val target = alternatives.values.view.filter(_.numberOfShows < showsPerElement).map((_, new Random().nextDouble())).toList.sortBy(_._2).take(maxElementsPerGo)
		target.foreach(e => {
			e._1.numberOfShows += 1
		})
		val selectedAlt = voteOnTargetsAndReturnWinner(target.map(_._1))
		selectedAlt.numberOfSelects += 1
	}

	protected def voteOnTargetsAndReturnWinner(target: List[AlternativeDetails]) = {
		val selected = driver.castSingleVote(target.map(_.alternative))
		val selectedAlt = target.find(_.alternative == selected).get //crash if not found is ok

		selectedAlt
	}

	protected case class AlternativeDetails(alternative: T, var numberOfShows: Int = 0, var numberOfSelects: Int = 0)

}

trait ContestDriver[T] {
	def alternatives: List[T]

	def castSingleVote(options: List[T]): T
}

class DefaultContestHCompDriver(
								   val alternatives: List[String],
								   hcompPortal: HCompPortalAdapter,
								   val question: String = "Please select the best element from this list",
								   val maxWait: Duration = 2 days) extends ContestDriver[String] {
	override def castSingleVote(options: List[String]): String = {
		hcompPortal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(question, options, 1, 1),
			maxWait
		).get.asInstanceOf[MultipleChoiceAnswer].selectedAnswer
	}
}