package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompInstructionsWithTuple
import ch.uzh.ifi.pdeboer.pplib.patterns.{IterativeRefinementExecutor, IRDefaultHCompDriver}
import ch.uzh.ifi.pdeboer.pplib.patterns.IRDefaultHCompDriver._
import ch.uzh.ifi.pdeboer.pplib.patterns.IterativeRefinementExecutor._
import ch.uzh.ifi.pdeboer.pplib.recombination._

/**
 * Created by pdeboer on 30/11/14.
 */
@PPLibProcess("create.refine.iterativerefinement")
class IterativeRefinementProcess(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[List[String], List[String]](params) {

	import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.IterativeRefinementProcess._

	override protected def run(data: List[String]): List[String] = {
		data.map(d => {
			val driver = new IRDefaultHCompDriver(portal, getParamUnsafe(TITLE_FOR_REFINEMENT),
				getParamUnsafe(QUESTION_FOR_REFINEMENT), getParamUnsafe(VOTING_PROCESS))
			val exec = new IterativeRefinementExecutor(d, driver, getParamUnsafe(ITERATION_COUNT))
			exec.refinedText
		})
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(TITLE_FOR_REFINEMENT, QUESTION_FOR_REFINEMENT, VOTING_PROCESS, ITERATION_COUNT)
}

object IterativeRefinementProcess {
	val TITLE_FOR_REFINEMENT = new ProcessParameter[String]("titleForRefinement", QuestionParam(), Some(List(DEFAULT_TITLE_FOR_REFINEMENT)))
	val QUESTION_FOR_REFINEMENT = new ProcessParameter[HCompInstructionsWithTuple]("questionForRefeinement", QuestionParam(), Some(List(DEFAULT_QUESTION_FOR_REFINEMENT)))
	val VOTING_PROCESS = new ProcessParameter[ProcessStub[List[String], String]]("votingProcess", WorkflowParam(), Some(List(DEFAULT_VOTING_PROCESS)))
	val ITERATION_COUNT = new ProcessParameter[Int]("iterationCount", OtherParam(), Some(List(DEFAULT_ITERATION_COUNT)))
}
