package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.patterns.SigmaPruner
import ch.uzh.ifi.pdeboer.pplib.process._

/**
 * Created by pdeboer on 27/11/14.
 */
@PPLibProcess("decide")
class SigmaPruningProcess(params: Map[String, Any]) extends ProcessStub[List[Double], List[Double]](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.SigmaPruningProcess._

	override protected def run(data: List[Double]): List[Double] = {
		val pruner = new SigmaPruner(data, NUM_SIGMAS.get)
		pruner.dataWithinRange
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(NUM_SIGMAS)
}

object SigmaPruningProcess {
	val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", OtherParam(), Some(List(3)))
}