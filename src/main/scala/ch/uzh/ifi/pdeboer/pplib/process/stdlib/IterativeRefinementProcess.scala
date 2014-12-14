package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTuple, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.patterns.IRDefaultHCompDriver._
import ch.uzh.ifi.pdeboer.pplib.patterns.IterativeRefinementExecutor._
import ch.uzh.ifi.pdeboer.pplib.patterns.{IRDefaultHCompDriver, IterativeRefinementExecutor}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

/**
 * Created by pdeboer on 30/11/14.
 */
@PPLibProcess("create.refine.iterativerefinement")
class IterativeRefinementProcess(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[List[String], List[String]](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.IterativeRefinementProcess._

	override protected def run(data: List[String]): List[String] = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		data.mpar.map(d => {
			val driver = new IRDefaultHCompDriver(portal, TITLE_FOR_REFINEMENT.get, QUESTION_FOR_REFINEMENT.get, VOTING_PROCESS_TYPE.get, QUESTION_PRICE.get, d)
			val exec = new IterativeRefinementExecutor(d, driver, MAX_ITERATION_COUNT.get, memoizer, d)
			exec.refinedText
		}).toList
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(TITLE_FOR_REFINEMENT, QUESTION_FOR_REFINEMENT, VOTING_PROCESS_TYPE, MAX_ITERATION_COUNT, QUESTION_PRICE, STRING_DIFFERENCE_THRESHOLD, TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD, VOTING_PROCESS_PARAMS)

}

object IterativeRefinementProcess {
	val TITLE_FOR_REFINEMENT = new ProcessParameter[String]("titleForRefinement", QuestionParam(), Some(List(DEFAULT_TITLE_FOR_REFINEMENT)))
	val QUESTION_FOR_REFINEMENT = new ProcessParameter[HCompInstructionsWithTuple]("questionForRefinement", QuestionParam(), Some(List(DEFAULT_QUESTION_FOR_REFINEMENT)))
	val VOTING_PROCESS_TYPE = new ProcessParameter[PassableProcessParam[List[String], String]]("votingProcess", WorkflowParam(), Some(List(DEFAULT_VOTING_PROCESS)))
	val VOTING_PROCESS_PARAMS = new ProcessParameter[Map[String, Any]]("votingProcessParams", WorkflowParam(), Some(List(Map.empty)))
	val MAX_ITERATION_COUNT = new ProcessParameter[Int]("iterationCount", OtherParam(), Some(List(DEFAULT_ITERATION_COUNT)))
	val QUESTION_PRICE = new ProcessParameter[HCompQueryProperties]("questionPrice", OtherParam(), Some(List(DEFAULT_QUESTION_PRICE)))
	val STRING_DIFFERENCE_THRESHOLD = new ProcessParameter[Int]("iterationStringDifferenceThreshold", OtherParam(), Some(List(DEFAULT_STRING_DIFFERENCE_THRESHOLD)))
	val TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD = new ProcessParameter[Int]("toleratedNumberOfIterationsBelowThreshold", OtherParam(), Some(List(DEFAULT_TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD)))
}
