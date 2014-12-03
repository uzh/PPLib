package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import java.util

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination._
import ch.uzh.ifi.pdeboer.pplib.util.MonteCarlo

import scala.reflect.internal.util.Collections
import scala.util.Random

/**
 * Created by pdeboer on 03/11/14.
 */
@PPLibProcess("decide.consensus.statistical")
class ContestWithStatisticalReductionProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[String], String](params) {

	import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.ContestWithStatisticalReductionProcess._

	protected val MONTECARLO_ITERATIONS: Int = 100000
	protected var votesCast = scala.collection.mutable.Map.empty[String, Int]

	override protected def run(data: List[String]): String = {
		do {
			//TODO refactor for parallelism
			val choice = castVote(data)
			votesCast += choice -> (votesCast.getOrElse(choice, 0) + 1)
		} while (minVotesForAgreement(data).getOrElse(Integer.MAX_VALUE) > itemWithMostVotes._2)

		itemWithMostVotes._1
	}

	def itemWithMostVotes: (String, Int) = {
		votesCast.maxBy(_._2)
	}

	protected def minVotesForAgreement(data: List[String]): Option[Int] = {
		MonteCarlo.minAgreementRequired(data.size, votesCast.values.sum, confidence, MONTECARLO_ITERATIONS)
	}

	def castVote(choices: List[String]) = {
		val instructions = INSTRUCTIONS_PARAMETER.get
		val auxString = AUX_STRING_PARAMETER.get
		val title = TITLE_PARAMETER.get
		val alternatives = if (SHUFFLE_CHOICES.get) Random.shuffle(choices) else choices


		portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(
				instructions.getInstructions(auxString),
				alternatives, 1, 1, title),
			HCompQueryProperties(paymentCents = 3)

		) match {
			case Some(a: MultipleChoiceAnswer) => a.selectedAnswer
			case _ => throw new IllegalStateException("didnt get any response") //TODO change me
		}
	}

	protected def confidence = CONFIDENCE_PARAMETER.get

	override val processCategoryNames: List[String] = List("selectbest.statistical")


	override def optionalParameters: List[ProcessParameter[_]] =
		List(AUX_STRING_PARAMETER, TITLE_PARAMETER, CONFIDENCE_PARAMETER, SHUFFLE_CHOICES, INSTRUCTIONS_PARAMETER)
}

object ContestWithStatisticalReductionProcess {
	val INSTRUCTIONS_PARAMETER = new ProcessParameter[HCompInstructionsWithTuple]("question", QuestionParam(), Some(List(HCompInstructionsWithTuple("Please select the item that fits best"))))
	val SHUFFLE_CHOICES = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))
	val AUX_STRING_PARAMETER = new ProcessParameter[String]("auxString", QuestionParam(), Some(List("")))
	val TITLE_PARAMETER = new ProcessParameter[String]("title", QuestionParam(), Some(List("Select Best Alternative")))
	val CONFIDENCE_PARAMETER = new ProcessParameter[java.lang.Double]("confidence", OtherParam(), Some(List(0.9d, 0.95d, 0.99d)))
}
