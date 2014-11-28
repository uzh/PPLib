package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination._

import scala.collection.generic.GenericTraversableTemplate
import scala.collection.{GenTraversable, GenIterable, GenSeq}
import scala.collection.parallel.ParSeq
import scala.collection.parallel.immutable.ParRange
import ch.uzh.ifi.pdeboer.pplib.util.U

/**
 * Created by pdeboer on 31/10/14.
 */
@PPLibProcess("decide.vote.fix")
class SelectBestAlternativeWithFixWorkerCount(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[String], String](params) {

	import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.SelectBestAlternativeWithFixWorkerCount._

	override def run(alternatives: List[String]): String = {
		val instructions = getParamUnsafe(INSTRUCTIONS_PARAMETER)
		val auxString = getParamUnsafe(AUX_STRING_PARAMETER)
		val title = getParamUnsafe(TITLE_PARAMETER)
		val workerCount = getParamUnsafe(WORKER_COUNT_PARAMETER)

		val answers = getCrowdWorkers(workerCount).map(w =>
			portal.sendQueryAndAwaitResult(
				MultipleChoiceQuery(instructions.getInstructions(auxString), alternatives, 1, 1, title))
			match {
				case Some(a: MultipleChoiceAnswer) => a.selectedAnswer
				case _ => throw new IllegalStateException("didnt get any response") //TODO change me
			}).toList

		answers.groupBy(s => s).maxBy(s => s._2.size)._1
	}

	override val recombinationCategoryNames: List[String] = List("selectbest.single")


	override def expectedParametersBeforeRun: List[ProcessParameter[_]] =
		List(INSTRUCTIONS_PARAMETER, WORKER_COUNT_PARAMETER)

	override def optionalParameters: List[ProcessParameter[_]] =
		List(AUX_STRING_PARAMETER,
			TITLE_PARAMETER)
}

object SelectBestAlternativeWithFixWorkerCount {
	val INSTRUCTIONS_PARAMETER = new ProcessParameter[HCompInstructionsWithTuple]("question", QuestionParam(), Some(List(HCompInstructionsWithTuple("Please select the option that fits best"))))
	val AUX_STRING_PARAMETER = new ProcessParameter[String]("auxString", QuestionParam(), Some(List("")))
	val TITLE_PARAMETER = new ProcessParameter[String]("title", QuestionParam(), Some(List("SelectBestAlternative")))
	val WORKER_COUNT_PARAMETER = new ProcessParameter[Int]("workerCount", WorkerCountParam(), Some(List(3, 5)))
}