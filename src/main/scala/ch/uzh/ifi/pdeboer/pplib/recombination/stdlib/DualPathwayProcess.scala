package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompInstructionsWithTuple, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.patterns.{DPHCompDriverDefaultComparisonInstructionsConfig, DualPathWayDefaultHCompDriver, DualPathwayExecutor}
import ch.uzh.ifi.pdeboer.pplib.recombination._
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.{Duration, _}


/**
 * Created by pdeboer on 04/11/14.
 */
@PPLibProcess("create.refine.dualpathway")
class DualPathwayProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[String], List[String]](params) {
	import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.DualPathwayProcess._

	/**
	 * list of paragraphs
	 * @param data
	 * @return
	 */
	override protected def run(data: List[String]): List[String] = {
		val driver = new DualPathWayDefaultHCompDriver(data, portal, QUESTION_OLD_PROCESSED_ELEMENT.get,
			QUESTION_NEW_PROCESSED_ELEMENT.get, QUESTION_PER_PROCESSING_TASK.get, QUESTION_PER_COMPARISON_TASK.get, TIMEOUT.get)

		val exec = new DualPathwayExecutor(driver, CHUNK_COUNT_TO_INCLUDE.get)
		exec.data.map(_.answer)
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(TIMEOUT,
			CHUNK_COUNT_TO_INCLUDE, QUESTION_OLD_PROCESSED_ELEMENT,
			QUESTION_NEW_PROCESSED_ELEMENT,
			QUESTION_PER_PROCESSING_TASK,
			QUESTION_PER_COMPARISON_TASK)
}

object DualPathwayProcess {
	val QUESTION_OLD_PROCESSED_ELEMENT = new ProcessParameter[HCompInstructionsWithTuple]("_old_el", QuestionParam(), Some(List(HCompInstructionsWithTuple("Is the following question answered correctly?"))))
	val QUESTION_NEW_PROCESSED_ELEMENT = new ProcessParameter[HCompInstructionsWithTuple]("_new_el", QuestionParam(), Some(List(HCompInstructionsWithTuple("Please provide an answer to the following question"))))
	val QUESTION_PER_PROCESSING_TASK = new ProcessParameter[String]("proc_task", QuestionParam(), Some(List("Please compare (and fix) the following elements")))
	val QUESTION_PER_COMPARISON_TASK = new ProcessParameter[DPHCompDriverDefaultComparisonInstructionsConfig]("question_comp_task", QuestionParam(), Some(List(new DPHCompDriverDefaultComparisonInstructionsConfig())))
	val TIMEOUT = new ProcessParameter[Duration]("timeout", OtherParam(), Some(List(2 days, 1 day)))
	val CHUNK_COUNT_TO_INCLUDE = new ProcessParameter[Integer]("chunk_count", OtherParam(), Some(List(2)))
}