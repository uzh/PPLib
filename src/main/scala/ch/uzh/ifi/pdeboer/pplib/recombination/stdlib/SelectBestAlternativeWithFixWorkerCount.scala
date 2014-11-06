package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination.{HCompPortalAccess, RecombinationParameter, RecombinationStub}

/**
 * Created by pdeboer on 31/10/14.
 */
class SelectBestAlternativeWithFixWorkerCount(params: Map[String, Any]) extends RecombinationStub[List[String], String](params) with HCompPortalAccess[List[String], String] {

	import SelectBestAlternativeWithFixWorkerCount._

	override def run(alternatives: List[String]): String = {
		val instructions = getParamUnsafe(INSTRUCTIONS_PARAMETER)
		val auxString = getParamUnsafe(AUX_STRING_PARAMETER)
		val title = getParamUnsafe(TITLE_PARAMETER)
		val workerCount = getParamUnsafe(WORKER_COUNT_PARAMETER)

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
		List(INSTRUCTIONS_PARAMETER,
			WORKER_COUNT_PARAMETER)
	}

	override def optionalParameters: List[RecombinationParameter[_]] =
		List(AUX_STRING_PARAMETER,
			TITLE_PARAMETER)
}

object SelectBestAlternativeWithFixWorkerCount {
	val INSTRUCTIONS_PARAMETER = new RecombinationParameter[HCompInstructionsWithData]("question")
	val AUX_STRING_PARAMETER = new RecombinationParameter[String]("auxString", Some(List("")))
	val TITLE_PARAMETER = new RecombinationParameter[String]("title", Some(List("SelectBestAlternative")))
	val WORKER_COUNT_PARAMETER = new RecombinationParameter[Int]("workerCount", Some(List(3, 5)))
}