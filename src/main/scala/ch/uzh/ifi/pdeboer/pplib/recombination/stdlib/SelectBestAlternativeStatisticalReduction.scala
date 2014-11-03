package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination.{RecombinationParameter, RecombinationStub}
import ch.uzh.ifi.pdeboer.pplib.util.MonteCarlo

/**
 * Created by pdeboer on 03/11/14.
 */
class SelectBestAlternativeStatisticalReduction(params: Map[String, Any]) extends RecombinationStub[List[String], String](params) {
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
		val portal = getParam(SelectBestAlternativeStatisticalReduction.PORTAL_PARAMETER).get
		val instructions = getParam(SelectBestAlternativeStatisticalReduction.INSTRUCTIONS_PARAMETER).get
		val auxString = getParam(SelectBestAlternativeStatisticalReduction.AUX_STRING_PARAMETER).get
		val title = getParam(SelectBestAlternativeStatisticalReduction.TITLE_PARAMETER).get

		portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(instructions.getInstructions(auxString), alternatives, 1, 1, title))
		match {
			case Some(a: MultipleChoiceAnswer) => a.selectedAnswer
			case _ => throw new IllegalStateException("didnt get any response") //TODO change me
		}
	}

	protected def confidence = getParam(SelectBestAlternativeStatisticalReduction.CONFIDENCE_PARAMETER).get

	override val recombinationCategoryNames: List[String] = List("selectbest.statistical")

	override def expectedParametersOnConstruction: List[RecombinationParameter[_]] = {
		List(SelectBestAlternativeStatisticalReduction.INSTRUCTIONS_PARAMETER)
	}

	override def optionalParameters: List[RecombinationParameter[_]] = List(SelectBestAlternativeStatisticalReduction.AUX_STRING_PARAMETER, SelectBestAlternativeStatisticalReduction.TITLE_PARAMETER, SelectBestAlternativeStatisticalReduction.CONFIDENCE_PARAMETER)

	override def expectedParametersBeforeRun: List[RecombinationParameter[_]] = List(SelectBestAlternativeStatisticalReduction.PORTAL_PARAMETER)

}

object SelectBestAlternativeStatisticalReduction {
	val PORTAL_PARAMETER = new RecombinationParameter[HCompPortalAdapter]("portal", candidateDefinitions = Some(HComp.allDefinedPortals))
	val INSTRUCTIONS_PARAMETER = new RecombinationParameter[HCompInstructionsWithData]("question")
	val AUX_STRING_PARAMETER = new RecombinationParameter[String]("auxString", Some(List("")))
	val TITLE_PARAMETER = new RecombinationParameter[String]("title", Some(List("SelectBestAlternative")))
	val CONFIDENCE_PARAMETER = new RecombinationParameter[java.lang.Double]("confidence", Some(List(0.9d, 0.95d, 0.99d)))
}
