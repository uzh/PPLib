package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompInstructionsWithData, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.patterns.{DPHCompDriverDefaultComparisonInstructionsConfig, DualPathWayDefaultHCompDriver, DualPathwayExecutor}
import ch.uzh.ifi.pdeboer.pplib.recombination.{RecombinationParameter, RecombinationStub}

import scala.concurrent.duration.{Duration, _}


/**
 * Created by pdeboer on 04/11/14.
 */
class DualPathwayProcess(params: Map[String, Any]) extends RecombinationStub[List[String], List[String]](params) {
	/**
	 * list of paragraphs
	 * @param data
	 * @return
	 */
	override protected def run(data: List[String]): List[String] = {
		val portal = getParam(DualPathwayProcess.PORTAL_PARAMETER).get
		val questionOldProcessedElement = getParam(DualPathwayProcess.QUESTION_OLD_PROCESSED_ELEMENT).get
		val questionNewProcessedElement = getParam(DualPathwayProcess.QUESTION_NEW_PROCESSED_ELEMENT).get
		val questionPerProcessingTask = getParam(DualPathwayProcess.QUESTION_PER_PROCESSING_TASK).get
		val questionPerComparisonTask = getParam(DualPathwayProcess.QUESTION_PER_COMPARISON_TASK).get
		val timeout = getParam(DualPathwayProcess.TIMEOUT).get
		val chunkCount = getParam(DualPathwayProcess.CHUNK_COUNT_TO_INCLUDE).get

		val driver = new DualPathWayDefaultHCompDriver(data, portal, questionOldProcessedElement,
			questionNewProcessedElement, questionPerProcessingTask, questionPerComparisonTask, timeout)

		val exec = new DualPathwayExecutor(driver, chunkCount)
		exec.data.map(_.answer)
	}

	override def expectedParametersBeforeRun: List[RecombinationParameter[_]] =
		List(DualPathwayProcess.PORTAL_PARAMETER,
			DualPathwayProcess.QUESTION_OLD_PROCESSED_ELEMENT,
			DualPathwayProcess.QUESTION_NEW_PROCESSED_ELEMENT,
			DualPathwayProcess.QUESTION_PER_PROCESSING_TASK,
			DualPathwayProcess.QUESTION_PER_COMPARISON_TASK)

	override def optionalParameters: List[RecombinationParameter[_]] =
		List(DualPathwayProcess.TIMEOUT,
			DualPathwayProcess.CHUNK_COUNT_TO_INCLUDE)
}

object DualPathwayProcess {
	val PORTAL_PARAMETER = new RecombinationParameter[HCompPortalAdapter]("portal", Some(HComp.allDefinedPortals))
	val QUESTION_OLD_PROCESSED_ELEMENT = new RecombinationParameter[HCompInstructionsWithData]("question_old_el")
	val QUESTION_NEW_PROCESSED_ELEMENT = new RecombinationParameter[HCompInstructionsWithData]("question_new_el")
	val QUESTION_PER_PROCESSING_TASK = new RecombinationParameter[String]("question_proc_task")
	val QUESTION_PER_COMPARISON_TASK = new RecombinationParameter[DPHCompDriverDefaultComparisonInstructionsConfig]("question_comp_task")
	val TIMEOUT = new RecombinationParameter[Duration]("timeout", Some(List(2 days)))
	val CHUNK_COUNT_TO_INCLUDE = new RecombinationParameter[Integer]("chunk_count", Some(List(2)))
}