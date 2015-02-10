package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTupleStringified, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.patterns.IRDefaultHCompDriver._
import ch.uzh.ifi.pdeboer.pplib.patterns.IterativeRefinementExecutor._
import ch.uzh.ifi.pdeboer.pplib.patterns.{IRDefaultHCompDriver, IterativeRefinementExecutor}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{ProcessParameter, PassableProcessParam, Patch}

import scala.xml.NodeSeq

/**
 * Created by pdeboer on 30/11/14.
 */
@PPLibProcess
class IterativeRefinementProcess(params: Map[String, Any] = Map.empty) extends CreateProcess[Patch, Patch](params) with HCompPortalAccess {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.IterativeRefinementProcess._

	override protected def run(data: Patch): Patch = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())

		logger.info("started refinement process for patch " + data)
		VOTING_PROCESS_TYPE.get.setParams(params, replace = false)

		val driver = new IRDefaultHCompDriver(portal, TITLE_FOR_REFINEMENT.get, QUESTION_FOR_REFINEMENT.get, VOTING_PROCESS_TYPE.get, QUESTION_PRICE.get, QUESTION_AUX.get, data.hashCode.toString)
		val exec = new IterativeRefinementExecutor(data.value, driver, MAX_ITERATION_COUNT.get, memoizer, data.hashCode.toString)
		data.duplicate(exec.refinedText)
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(QUESTION_AUX, TITLE_FOR_REFINEMENT, QUESTION_FOR_REFINEMENT, VOTING_PROCESS_TYPE, MAX_ITERATION_COUNT, QUESTION_PRICE, STRING_DIFFERENCE_THRESHOLD, TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD)

}

object IterativeRefinementProcess {
	val TITLE_FOR_REFINEMENT = new ProcessParameter[String]("titleForRefinement", Some(List(DEFAULT_TITLE_FOR_REFINEMENT)))
	val QUESTION_FOR_REFINEMENT = new ProcessParameter[HCompInstructionsWithTupleStringified]("questionForRefinement", Some(List(DEFAULT_QUESTION_FOR_REFINEMENT)))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", Some(List(None)))

	val VOTING_PROCESS_TYPE = new ProcessParameter[PassableProcessParam[List[Patch], Patch]]("votingProcess", Some(List(DEFAULT_VOTING_PROCESS)))
	val MAX_ITERATION_COUNT = new ProcessParameter[Int]("iterationCount", Some(List(DEFAULT_ITERATION_COUNT)))
	val QUESTION_PRICE = new ProcessParameter[HCompQueryProperties]("questionPrice", Some(List(DEFAULT_QUESTION_PRICE)))
	val STRING_DIFFERENCE_THRESHOLD = new ProcessParameter[Int]("iterationStringDifferenceThreshold", Some(List(DEFAULT_STRING_DIFFERENCE_THRESHOLD)))
	val TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD = new ProcessParameter[Int]("toleratedNumberOfIterationsBelowThreshold", Some(List(DEFAULT_TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD)))
}
