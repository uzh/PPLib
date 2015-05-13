package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.patterns.IterativeRefinementExecutor._
import ch.uzh.ifi.pdeboer.pplib.patterns.{IRDefaultHCompDriver, IterativeRefinementExecutor}
import ch.uzh.ifi.pdeboer.pplib.process.entities._

/**
 * Created by pdeboer on 30/11/14.
 */
@PPLibProcess
class IterativeRefinementProcess(params: Map[String, Any] = Map.empty) extends CreateProcess[Patch, Patch](params) with HCompPortalAccess with InstructionHandler {

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.IterativeRefinementProcess._

	override protected def run(data: Patch): Patch = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())

		logger.info("started refinement process for patch " + data)
		VOTING_PROCESS_TYPE.get.setParams(params, replace = false)

		val driver = new IRDefaultHCompDriver(portal, instructionTitle, instructions, VOTING_PROCESS_TYPE.get, QUESTION_PRICE.get, QUESTION_AUX.get, MEMOIZER_NAME.get.map(m => data.hashCode().toString))
		val exec = new IterativeRefinementExecutor(data.value, driver, MAX_ITERATIONS.get, memoizer, data.hashCode.toString)
		data.duplicate(exec.refinedText)
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(VOTING_PROCESS_TYPE, STRING_DIFFERENCE_THRESHOLD, TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD, MAX_ITERATIONS)
}

object IterativeRefinementProcess {
	val VOTING_PROCESS_TYPE = new ProcessParameter[PassableProcessParam[DecideProcess[List[Patch], Patch]]]("votingProcess", None)
	val STRING_DIFFERENCE_THRESHOLD = new ProcessParameter[Int]("iterationStringDifferenceThreshold", Some(List(DEFAULT_STRING_DIFFERENCE_THRESHOLD)))
	val TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD = new ProcessParameter[Int]("toleratedNumberOfIterationsBelowThreshold", Some(List(DEFAULT_TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD)))
}
