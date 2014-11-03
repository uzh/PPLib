package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination.{RecombinationParameter, RecombinationStub}

/**
 * Created by pdeboer on 31/10/14.
 */
class SelectBestAlternativeWithFixWorkerCount(params: Map[String, Any]) extends RecombinationStub[List[String], String](params) {
	override def run(alternatives: List[String]): String = {
		val portal = getParam(SelectBestAlternativeWithFixWorkerCount.PORTAL_PARAMETER).get
		val instructions = getParam(SelectBestAlternativeWithFixWorkerCount.INSTRUCTIONS_PARAMETER).get
		val auxString = getParam(SelectBestAlternativeWithFixWorkerCount.AUX_STRING_PARAMETER).get
		val title = getParam(SelectBestAlternativeWithFixWorkerCount.TITLE_PARAMETER).get
		val workerCount = getParam(SelectBestAlternativeWithFixWorkerCount.WORKER_COUNT_PARAMETER).get

		val answers = (1 to workerCount).map(e =>
			portal.sendQueryAndAwaitResult(
				MultipleChoiceQuery(instructions.getInstructions(auxString), alternatives, 1, 1, title))
			match {
				case Some(a: MultipleChoiceAnswer) => a.selectedAnswer
				case _ => throw new IllegalStateException("didnt get any response") //TODO change me
			}).toList

		answers.groupBy(s => s).maxBy(s => s._2.size)._1
	}

	override val recombinationCategoryNames: List[String] = List("selectbest.single")

	override def expectedParametersOnConstruction: List[RecombinationParameter[_]] = {
		List(SelectBestAlternativeWithFixWorkerCount.INSTRUCTIONS_PARAMETER,
			SelectBestAlternativeWithFixWorkerCount.WORKER_COUNT_PARAMETER)
	}

	override def optionalParameters: List[RecombinationParameter[_]] =
		List(SelectBestAlternativeWithFixWorkerCount.AUX_STRING_PARAMETER,
			SelectBestAlternativeWithFixWorkerCount.TITLE_PARAMETER)

	override def expectedParametersBeforeRun: List[RecombinationParameter[_]] =
		List(SelectBestAlternativeWithFixWorkerCount.PORTAL_PARAMETER)
}

object SelectBestAlternativeWithFixWorkerCount {
	val PORTAL_PARAMETER = new RecombinationParameter[HCompPortalAdapter]("portal", candidateDefinitions = Some(HComp.allDefinedPortals))
	val INSTRUCTIONS_PARAMETER = new RecombinationParameter[HCompInstructionsWithData]("question")
	val AUX_STRING_PARAMETER = new RecombinationParameter[String]("auxString", Some(List("")))
	val TITLE_PARAMETER = new RecombinationParameter[String]("title", Some(List("SelectBestAlternative")))
	val WORKER_COUNT_PARAMETER = new RecombinationParameter[Int]("workerCount", Some(List(3, 5)))
}