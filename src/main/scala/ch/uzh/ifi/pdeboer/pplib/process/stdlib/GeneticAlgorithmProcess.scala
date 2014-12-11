package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompInstructionsWithTuple
import ch.uzh.ifi.pdeboer.pplib.patterns.GeneticAlgorithmHCompDriver._
import ch.uzh.ifi.pdeboer.pplib.patterns.{GAIterationLimitTerminator, GeneticAlgorithmExecutor, GeneticAlgorithmHCompDriver}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.StringPatch

/**
 * Created by pdeboer on 10/12/14.
 */
@PPLibProcess("create.refine.geneticalgorithm")
class GeneticAlgorithmProcess(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[List[String], List[String]](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.GeneticAlgorithmProcess._

	override protected def run(data: List[String]): List[String] = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		data.map(d => {
			val driver = new GeneticAlgorithmHCompDriver(portal, new StringPatch(d))
			val terminator: GAIterationLimitTerminator = new GAIterationLimitTerminator(10)
			val exec = memoizer.memWithReinitialization(d + "ga_exec")(
				new GeneticAlgorithmExecutor(driver, terminator, memoizer = memoizer, memoizerPrefix = d)) { exec =>
				exec.driver = driver
				exec.terminationCriterion = terminator
				exec
			}
			exec.refinedData(0).toString
		})
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(ELITISM, RECOMBINATION_FRACTION, MUTATION_FRACTION) ::: super.optionalParameters
}

object GeneticAlgorithmProcess {
	val ELITISM = new ProcessParameter[Double]("elitism", OtherParam(), Some(List(.1d)))
	val RECOMBINATION_FRACTION = new ProcessParameter[Double]("recombinationFraction", OtherParam(), Some(List(.8d)))
	val MUTATION_FRACTION = new ProcessParameter[Double]("mutationFraction", OtherParam(), Some(List(.1d)))
	val COMBINE_QUESTION = new ProcessParameter[HCompInstructionsWithTuple]("combineQuestion", QuestionParam(), Some(List(DEFAULT_COMBINE_QUESTION)))
	val COMBINE_TITLE = new ProcessParameter[String]("combineTitle", QuestionParam(), Some(List(DEFAULT_COMBINE_TITLE)))
	val MUTATE_QUESTION = new ProcessParameter[HCompInstructionsWithTuple]("mutateQuestion", QuestionParam(), Some(List(DEFAULT_MUTATE_QUESTION)))
	val MUTATE_TITLE = new ProcessParameter[String]("mutateTitle", QuestionParam(), Some(List(DEFAULT_MUTATE_TITLE)))
	val RATING_QUESTION = new ProcessParameter[HCompInstructionsWithTuple]("ratingQuestion", QuestionParam(), Some(List(DEFAULT_RATING_QUESTION)))
	val RATING_TITLE = new ProcessParameter[String]("ratingTitle", QuestionParam(), Some(List(DEFAULT_RATING_TITLE)))
}
