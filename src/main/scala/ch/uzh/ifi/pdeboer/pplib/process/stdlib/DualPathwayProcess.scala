package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTupleStringified, HCompInstructionsWithTupleStringified$}
import ch.uzh.ifi.pdeboer.pplib.patterns.{DPHCompDriverDefaultComparisonInstructionsConfig, DualPathWayDefaultHCompDriver, DualPathwayExecutor}
import ch.uzh.ifi.pdeboer.pplib.process._

import scala.concurrent.duration.{Duration, _}


/**
 * Created by pdeboer on 04/11/14.
 */
@PPLibProcess("create.refine.dualpathway")
class DualPathwayProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[String], List[String]](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.DualPathwayProcess._

	/**
	 * list of paragraphs
	 * @param data
	 * @return
	 */
	override protected def run(data: List[String]): List[String] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())

		val driver = new DualPathWayDefaultHCompDriver(data, portal, QUESTION_OLD_PROCESSED_ELEMENT.get,
			QUESTION_NEW_PROCESSED_ELEMENT.get, QUESTION_PER_PROCESSING_TASK.get, QUESTION_PER_COMPARISON_TASK.get, TIMEOUT.get)

		val exec = memoizer.memWithReinitialization("exec")(new DualPathwayExecutor(driver, CHUNK_COUNT_TO_INCLUDE.get))(exec => {
			exec.driver = driver
			exec
		})
		exec.result.map(_.answer)
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(TIMEOUT,
			CHUNK_COUNT_TO_INCLUDE, QUESTION_OLD_PROCESSED_ELEMENT,
			QUESTION_NEW_PROCESSED_ELEMENT,
			QUESTION_PER_PROCESSING_TASK,
			QUESTION_PER_COMPARISON_TASK)
}

object DualPathwayProcess {
	val QUESTION_OLD_PROCESSED_ELEMENT = new ProcessParameter[HCompInstructionsWithTupleStringified]("_old_el", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Other crowd workers have been given this sentence:", "And refined it to this state:", "Please check their work and add any refinements you think are necessary"))))
	val QUESTION_NEW_PROCESSED_ELEMENT = new ProcessParameter[HCompInstructionsWithTupleStringified]("_new_el", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Please refine the following sentence"))))
	val QUESTION_PER_PROCESSING_TASK = new ProcessParameter[String]("proc_task", QuestionParam(), Some(List("Please fix up the following sentences")))
	val QUESTION_PER_COMPARISON_TASK = new ProcessParameter[DPHCompDriverDefaultComparisonInstructionsConfig]("question_comp_task", QuestionParam(), Some(List(new DPHCompDriverDefaultComparisonInstructionsConfig())))
	val TIMEOUT = new ProcessParameter[Duration]("timeout", OtherParam(), Some(List(14 days)))
	val CHUNK_COUNT_TO_INCLUDE = new ProcessParameter[Integer]("chunk_count", OtherParam(), Some(List(2)))
}