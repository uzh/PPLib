

package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.IRDefaultHCompDriver._
import ch.uzh.ifi.pdeboer.pplib.patterns.IterativeRefinementExecutor._
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

import scala.xml.NodeSeq

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
		val newState = memoizer.mem(memoizerPrefix + "refinement" + stepNumber)(driver.refine(textToRefine, currentState, stepNumber))
		logger.info(s"asked crowd workers to refine '$currentState'. Answer was $newState <--")
		currentState = memoizer.mem(memoizerPrefix + "bestRefinement" + stepNumber)(driver.selectBestRefinement(List(currentState, newState)))
		iterationWatcher.addIteration(currentState)
		logger.info("crowd workers determined the following state to be better: " + currentState)
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
	def refine(originalToRefine: T, refinementState: T, iterationId: Int): String

	def selectBestRefinement(candidates: List[T]): String
}

class IRDefaultHCompDriver(portal: HCompPortalAdapter, titleForRefinementQuestion: String = DEFAULT_TITLE_FOR_REFINEMENT, questionForRefinement: HCompInstructionsWithTuple = DEFAULT_QUESTION_FOR_REFINEMENT, votingProcessParam: PassableProcessParam[DecideProcess[List[Patch], Patch]], questionPricing: HCompQueryProperties = DEFAULT_QUESTION_PRICE, questionAux: Option[NodeSeq] = None, memoizerPrefix: Option[String] = None) extends IterativeRefinementDriver[String] {
	override def refine(originalTextToRefine: String, currentRefinementState: String, iterationId: Int): String = {
		val q = FreetextQuery(
			questionForRefinement.getInstructions(originalTextToRefine, currentRefinementState, questionAux.getOrElse(Nil)), "", titleForRefinementQuestion + iterationId)
		val answer = portal.sendQueryAndAwaitResult(q, properties = questionPricing).get.asInstanceOf[FreetextAnswer]
		answer.answer
	}

	override def selectBestRefinement(candidates: List[String]): String = {
		val candidatesDistinct = candidates.distinct

		val memPrefixInParams: String = votingProcessParam.getParam[Option[String]](
			DefaultParameters.MEMOIZER_NAME.key).getOrElse(Some("")).getOrElse("")

		val lowerPriorityParams = Map(DefaultParameters.PORTAL_PARAMETER.key -> portal)
		val higherPriorityParams = Map(DefaultParameters.MEMOIZER_NAME.key -> memoizerPrefix.map(m => m.hashCode + "selectbest" + memPrefixInParams))

		val votingProcess = votingProcessParam.create(lowerPriorityParams, higherPriorityParams)
		votingProcess.process(candidatesDistinct.map(c => new Patch(c))).value
	}
}

object IRDefaultHCompDriver {
	val DEFAULT_TITLE_FOR_REFINEMENT: String = "Please refine the following sentence"
	val DEFAULT_QUESTION_FOR_REFINEMENT = HCompInstructionsWithTupleStringified("Other crowd workers have refined the text below", "To this state: ", questionAfterTuples = "Please refine it further. We don't like REJECTIONS, but due to bad experiences we had to deploy a system that detects malicious / unchanged sentences and will reject your answer if it's deemed unhelpful by both, the software and afterwards a human - so please don't cheat. ")
	val DEFAULT_QUESTION_FOR_VOTING = HCompInstructionsWithTupleStringified("Other crowd workers have written the following refinements to the sentence below. Please select the one you like the best. If there are multiple sentences in one item, please ensure to NOT pick that one as there should be only one", questionAfterTuples = "We will only accept your first HIT in this group.")
	val DEFAULT_TITLE_FOR_VOTING = "Choose the best sentence"
	val DEFAULT_WORKER_COUNT_FOR_VOTING = 3

	val DEFAULT_QUESTION_PRICE = HCompQueryProperties()
}
