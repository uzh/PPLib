package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompInstructionsWithData, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.patterns.{DPHCompDriverDefaultComparisonInstructionsConfig, DualPathWayDefaultHCompDriver, DualPathwayExecutor}
import ch.uzh.ifi.pdeboer.pplib.recombination.{HCompPortalAccess, RecombinationParameter, RecombinationStub}

import scala.concurrent.duration.{Duration, _}


/**
 * Created by pdeboer on 04/11/14.
 */
class DualPathwayProcess(params: Map[String, Any]) extends RecombinationStub[List[String], List[String]](params) with HCompPortalAccess[List[String], List[String]] {

	import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.DualPathwayProcess._

	/**
	 * list of paragraphs
	 * @param data
	 * @return
	 */
	override protected def run(data: List[String]): List[String] = {
		val questionOldProcessedElement = getParamUnsafe(QUESTION_OLD_PROCESSED_ELEMENT)
		val questionNewProcessedElement = getParamUnsafe(QUESTION_NEW_PROCESSED_ELEMENT)
		val questionPerProcessingTask = getParamUnsafe(QUESTION_PER_PROCESSING_TASK)
		val questionPerComparisonTask = getParamUnsafe(QUESTION_PER_COMPARISON_TASK)
		val timeout = getParamUnsafe(TIMEOUT)
		val chunkCount = getParamUnsafe(CHUNK_COUNT_TO_INCLUDE)

		val driver = new DualPathWayDefaultHCompDriver(data, portal, questionOldProcessedElement,
			questionNewProcessedElement, questionPerProcessingTask, questionPerComparisonTask, timeout)

		val exec = new DualPathwayExecutor(driver, chunkCount)
		exec.data.map(_.answer)
	}

	override def expectedParametersBeforeRun: List[RecombinationParameter[_]] =
		List(QUESTION_OLD_PROCESSED_ELEMENT,
			QUESTION_NEW_PROCESSED_ELEMENT,
			QUESTION_PER_PROCESSING_TASK,
			QUESTION_PER_COMPARISON_TASK)

	override def optionalParameters: List[RecombinationParameter[_]] =
		List(TIMEOUT,
			CHUNK_COUNT_TO_INCLUDE)
}

object DualPathwayProcess {
	val QUESTION_OLD_PROCESSED_ELEMENT = new RecombinationParameter[HCompInstructionsWithData]("question_old_el")
	val QUESTION_NEW_PROCESSED_ELEMENT = new RecombinationParameter[HCompInstructionsWithData]("question_new_el")
	val QUESTION_PER_PROCESSING_TASK = new RecombinationParameter[String]("question_proc_task")
	val QUESTION_PER_COMPARISON_TASK = new RecombinationParameter[DPHCompDriverDefaultComparisonInstructionsConfig]("question_comp_task")
	val TIMEOUT = new RecombinationParameter[Duration]("timeout", Some(List(2 days)))
	val CHUNK_COUNT_TO_INCLUDE = new RecombinationParameter[Integer]("chunk_count", Some(List(2)))
}