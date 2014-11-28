package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompInstructionsWithTuple
import ch.uzh.ifi.pdeboer.pplib.patterns.SigmaPruner
import ch.uzh.ifi.pdeboer.pplib.recombination.{ProcessStub, ProcessParameter, ProcessStubWithHCompPortalAccess, PPLibProcess}

/**
 * Created by pdeboer on 27/11/14.
 */
@PPLibProcess("decide")
class SigmaPruningProcess(params: Map[String, Any]) extends ProcessStub[List[Double], List[Double]](params) {

	import SigmaPruningProcess._

	override protected def run(data: List[Double]): List[Double] = {
		val pruner = new SigmaPruner(data, getParamUnsafe(NUM_SIGMAS))
		pruner.dataWithinRange
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(NUM_SIGMAS)
}

object SigmaPruningProcess {
	val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", Some(List(3)))
}