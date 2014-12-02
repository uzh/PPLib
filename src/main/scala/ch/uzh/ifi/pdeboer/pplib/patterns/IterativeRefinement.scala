

package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination.ProcessStub
import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.SelectBestAlternativeWithFixWorkerCount
import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.SelectBestAlternativeWithFixWorkerCount._
import IterativeRefinementExecutor._
import IRDefaultHCompDriver._

/**
 * Created by pdeboer on 30/11/14.
 */
class IterativeRefinementExecutor(val textToRefine: String, val driver: IterativeRefinementDriver[String], val numIterations: Int = DEFAULT_ITERATION_COUNT) {
	assert(numIterations > 0)

	var currentState: String = textToRefine

	def step(): Unit = {
		val newState = driver.refine(textToRefine, currentState)
		currentState = driver.selectBestRefinement(List(currentState, newState))
	}

	lazy val refinedText: String = {
		for (i <- 1 to numIterations) {
			step()
		}
		currentState
	}
}

object IterativeRefinementExecutor {
	val DEFAULT_ITERATION_COUNT: Int = 5
}

trait IterativeRefinementDriver[T] {
	def refine(originalToRefine: T, refinementState: T): String

	def selectBestRefinement(candidates: List[T]): String
}

class IRDefaultHCompDriver(val portal: HCompPortalAdapter,
						   val titleForRefinementQuestion: String = DEFAULT_TITLE_FOR_REFINEMENT,
						   val questionForRefinement: HCompInstructionsWithTuple = DEFAULT_QUESTION_FOR_REFINEMENT,
						   val votingProcess: ProcessStub[List[String], String] = DEFAULT_VOTING_PROCESS) extends IterativeRefinementDriver[String] {

	override def refine(originalTextToRefine: String, currentRefinementState: String): String = {
		val q = FreetextQuery(questionForRefinement.getInstructions(originalTextToRefine), currentRefinementState, titleForRefinementQuestion)
		val answer = portal.sendQueryAndAwaitResult(q, properties = HCompQueryProperties(5)).get.asInstanceOf[FreetextAnswer]
		answer.answer
	}

	override def selectBestRefinement(candidates: List[String]): String = {
		votingProcess.process(candidates)
	}
}

object IRDefaultHCompDriver {
	val DEFAULT_TITLE_FOR_REFINEMENT: String = "Please refine the following sentence"
	val DEFAULT_QUESTION_FOR_REFINEMENT = HCompInstructionsWithTuple("Other crowd workers have refined the text below to the state you see in the text field. Please refine it further")
	val DEFAULT_QUESTION_FOR_VOTING = HCompInstructionsWithTuple("Other crowd workers have written the following refinements to the sentence below. Please select the one you like more")
	val DEFAULT_TITLE_FOR_VOTING = "Choose the best sentence"
	val DEFAULT_WORKER_COUNT_FOR_VOTING = 3
	val DEFAULT_VOTING_PROCESS = new SelectBestAlternativeWithFixWorkerCount(Map(
		INSTRUCTIONS_PARAMETER.key -> DEFAULT_QUESTION_FOR_VOTING,
		TITLE_PARAMETER.key -> DEFAULT_TITLE_FOR_VOTING,
		WORKER_COUNT_PARAMETER.key -> DEFAULT_WORKER_COUNT_FOR_VOTING
	))
}
