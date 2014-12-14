

package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.IRDefaultHCompDriver._
import ch.uzh.ifi.pdeboer.pplib.patterns.IterativeRefinementExecutor._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithFixWorkerCountProcess
import ch.uzh.ifi.pdeboer.pplib.process.{NoProcessMemoizer, ProcessMemoizer, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

/**
 * Created by pdeboer on 30/11/14.
 */
class IterativeRefinementExecutor(val textToRefine: String,
								  val driver: IterativeRefinementDriver[String],
								  val maxIterations: Int = DEFAULT_ITERATION_COUNT,
								  val memoizer: ProcessMemoizer = new NoProcessMemoizer(),
								  val memoizerPrefix: String = "",
								  val stringDifferenceThreshold: Int = DEFAULT_STRING_DIFFERENCE_THRESHOLD,
								  val toleratedNumberOfIterationsBelowThreshold: Int = DEFAULT_TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD) extends LazyLogger {
	assert(maxIterations > 0)

	var currentState: String = textToRefine
	protected val iterationWatcher = new IterationWatcher(textToRefine, stringDifferenceThreshold, toleratedNumberOfIterationsBelowThreshold)

	def step(stepNumber: Int): Unit = {
		val newState = memoizer.mem(memoizerPrefix + "refinement" + stepNumber)(driver.refine(textToRefine, currentState))
		iterationWatcher.addIteration(newState)
		if (iterationWatcher.shouldRunAnotherIteration)
			currentState = memoizer.mem(memoizerPrefix + "bestRefinement" + stepNumber)(driver.selectBestRefinement(List(currentState, newState)))
		else currentState = newState
	}

	lazy val refinedText: String = {
		for (i <- 1 to maxIterations) {
			if (iterationWatcher.shouldRunAnotherIteration)
				step(i)
			else logger.info(s"ending IR early due to unchanging answer. Step $i")
		}
		currentState
	}
}

object IterativeRefinementExecutor {
	val DEFAULT_ITERATION_COUNT: Int = 5
	val DEFAULT_STRING_DIFFERENCE_THRESHOLD = 1
	val DEFAULT_TOLERATED_NUMBER_OF_ITERATIONS_BELOW_THRESHOLD = 2
}

trait IterativeRefinementDriver[T] {
	def refine(originalToRefine: T, refinementState: T): String

	def selectBestRefinement(candidates: List[T]): String
}

class IRDefaultHCompDriver(portal: HCompPortalAdapter, titleForRefinementQuestion: String = DEFAULT_TITLE_FOR_REFINEMENT, questionForRefinement: HCompInstructionsWithTuple = DEFAULT_QUESTION_FOR_REFINEMENT, votingProcessType: Class[_ <: ProcessStub[List[String], String]] = DEFAULT_VOTING_PROCESS, votingProcessMap: Map[String, Any] = Map.empty, questionPricing: HCompQueryProperties = DEFAULT_QUESTION_PRICE, memoizerPrefix: String = "") extends IterativeRefinementDriver[String] {

	override def refine(originalTextToRefine: String, currentRefinementState: String): String = {
		val q = FreetextQuery(questionForRefinement.getInstructions(originalTextToRefine), currentRefinementState, titleForRefinementQuestion)
		val answer = portal.sendQueryAndAwaitResult(q, properties = questionPricing).get.asInstanceOf[FreetextAnswer]
		answer.answer
	}

	override def selectBestRefinement(candidates: List[String]): String = {
		val votingProcess = ProcessStub.create[List[String], String](votingProcessType, votingProcessMap ++ Map(
			ProcessStub.MEMOIZER_NAME.key -> Some(memoizerPrefix + "selectbest")
		))

		votingProcess.process(candidates)
	}
}

object IRDefaultHCompDriver {
	val DEFAULT_TITLE_FOR_REFINEMENT: String = "Please refine the following sentence"
	val DEFAULT_QUESTION_FOR_REFINEMENT = HCompInstructionsWithTuple("Other crowd workers have refined the text below to the state you see in the text field. Please refine it further.", "If you're unhappy with the current state, just copy&paste the original sentence and fix it.", questionAfterTuples = "We will accept only your first HIT in this group, please don't accept any other. We don't like REJECTIONS, but due to bad experiences we had to deploy a system that detects malicious / unchanged sentences and will reject your answer if it's deemed unhelpful by both, the software and afterwards a human - so please don't cheat. ")
	val DEFAULT_QUESTION_FOR_VOTING = HCompInstructionsWithTuple("Other crowd workers have written the following refinements to the sentence below. Please select the one you like more", questionAfterTuples = "We will only accept your first HIT in this group.")
	val DEFAULT_TITLE_FOR_VOTING = "Choose the best sentence"
	val DEFAULT_WORKER_COUNT_FOR_VOTING = 1
	val DEFAULT_VOTING_PROCESS = classOf[ContestWithFixWorkerCountProcess]
	val DEFAULT_VOTING_PROCESS_PARAMS = Map(
		ContestWithFixWorkerCountProcess.INSTRUCTIONS.key -> DEFAULT_QUESTION_FOR_VOTING,
		ContestWithFixWorkerCountProcess.TITLE.key -> DEFAULT_TITLE_FOR_VOTING,
		ContestWithFixWorkerCountProcess.WORKER_COUNT.key -> DEFAULT_WORKER_COUNT_FOR_VOTING,
		ProcessStub.MEMOIZER_NAME.key -> Some("IR_voting")
	)
	val DEFAULT_QUESTION_PRICE = HCompQueryProperties()
}
