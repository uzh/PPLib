package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination.{RecombinationStubWithHCompPortalAccess, RecombinationParameter, RecombinationStub}
import ch.uzh.ifi.pdeboer.pplib.util.MonteCarlo

/**
 * Created by pdeboer on 03/11/14.
 */
class SelectBestAlternativeStatisticalReduction(params: Map[String, Any]) extends RecombinationStubWithHCompPortalAccess[List[String], String](params) {

	import SelectBestAlternativeStatisticalReduction._

	protected val MONTECARLO_ITERATIONS: Int = 100000
	protected var votesCast = scala.collection.mutable.Map.empty[String, Int]

	override protected def run(data: List[String]): String = {
		do {
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

	def castVote(alternatives: List[String]) = {
		val instructions = getParamUnsafe(INSTRUCTIONS_PARAMETER)
		val auxString = getParamUnsafe(AUX_STRING_PARAMETER)
		val title = getParamUnsafe(TITLE_PARAMETER)

		portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(instructions.getInstructions(auxString), alternatives, 1, 1, title))
		match {
			case Some(a: MultipleChoiceAnswer) => a.selectedAnswer
			case _ => throw new IllegalStateException("didnt get any response") //TODO change me
		}
	}

	protected def confidence = getParamUnsafe(CONFIDENCE_PARAMETER)

	override val recombinationCategoryNames: List[String] = List("selectbest.statistical")

	override def expectedParametersOnConstruction: List[RecombinationParameter[_]] = {
		List(INSTRUCTIONS_PARAMETER)
	}

	override def optionalParameters: List[RecombinationParameter[_]] =
		List(AUX_STRING_PARAMETER, TITLE_PARAMETER, CONFIDENCE_PARAMETER)
}

object SelectBestAlternativeStatisticalReduction {
	val INSTRUCTIONS_PARAMETER = new RecombinationParameter[HCompInstructionsWithData]("question")
	val AUX_STRING_PARAMETER = new RecombinationParameter[String]("auxString", Some(List("")))
	val TITLE_PARAMETER = new RecombinationParameter[String]("title", Some(List("SelectBestAlternative")))
	val CONFIDENCE_PARAMETER = new RecombinationParameter[java.lang.Double]("confidence", Some(List(0.9d, 0.95d, 0.99d)))
}
