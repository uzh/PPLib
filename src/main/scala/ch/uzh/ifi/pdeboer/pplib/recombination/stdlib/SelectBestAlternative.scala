package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination.{RecombinationParameter, RecombinationStub}

/**
 * Created by pdeboer on 31/10/14.
 */
class SelectBestAlternative(params: Map[String, AnyRef]) extends RecombinationStub[List[String], String](params) {
	override def run(alternatives: List[String]): String = {
		val portal = getParam(SelectBestAlternative.PORTAL_PARAMETER).get
		val instructions = getParam(SelectBestAlternative.INSTRUCTIONS_PARAMETER).get
		val auxString = getParam(SelectBestAlternative.AUX_STRING_PARAMETER).get
		val title = getParam(SelectBestAlternative.TITLE_PARAMETER).get

		portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(instructions.getInstructions(auxString), alternatives, 1, 1, title))
		match {
			case Some(a: MultipleChoiceAnswer) => a.selectedAnswer
			case _ => throw new IllegalStateException("didnt get any response") //TODO change me
		}
	}

	override val recombinationCategories: List[String] = List("selectbest.single")

	override def expectedParametersOnConstruction: List[RecombinationParameter[_]] = {
		List(SelectBestAlternative.INSTRUCTIONS_PARAMETER)
	}

	override def optionalParameters: List[RecombinationParameter[_]] = List(SelectBestAlternative.AUX_STRING_PARAMETER, SelectBestAlternative.TITLE_PARAMETER)

	override def expectedParametersBeforeRun: List[RecombinationParameter[_]] = List(SelectBestAlternative.PORTAL_PARAMETER)
}

object SelectBestAlternative {
	val PORTAL_PARAMETER = new RecombinationParameter[HCompPortalAdapter]("portal", candidateDefinitions = Some(HComp.allDefinedPortals))
	val INSTRUCTIONS_PARAMETER = new RecombinationParameter[HCompInstructionsWithData]("question")
	val AUX_STRING_PARAMETER = new RecombinationParameter[String]("auxString", Some(List("")))
	val TITLE_PARAMETER = new RecombinationParameter[String]("title", Some(List("SelectBestAlternative")))
}