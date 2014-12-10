package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.patterns.{GAIterationLimitTerminator, GeneticAlgorithmExecutor, GeneticAlgorithmHCompDriver}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.StringPatch

/**
 * Created by pdeboer on 10/12/14.
 */
@PPLibProcess("create.refine.geneticalgorithm")
class GeneticAlgorithmProcess(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[String, String](params) {

	import GeneticAlgorithmProcess._

	override protected def run(data: String): String = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		val driver = new GeneticAlgorithmHCompDriver(portal, new StringPatch(data))
		val terminator: GAIterationLimitTerminator = new GAIterationLimitTerminator(10)
		val exec = memoizer.memWithReinitialization("ga_exec")(
			new GeneticAlgorithmExecutor(driver, terminator, memoizer = memoizer)) { exec =>
			exec.driver = driver
			exec.terminationCriterion = terminator
			exec
		}
		exec.refinedData(0).toString
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(ELITISM, RECOMBINATION_FRACTION, MUTATION_FRACTION) ::: super.optionalParameters
}

object GeneticAlgorithmProcess {
	val ELITISM = new ProcessParameter[Double]("elitism", OtherParam(), Some(List(.1d)))
	val RECOMBINATION_FRACTION = new ProcessParameter[Double]("recombinationFraction", OtherParam(), Some(List(.8d)))
	val MUTATION_FRACTION = new ProcessParameter[Double]("mutationFraction", OtherParam(), Some(List(.1d)))
	//TODO add others
}
