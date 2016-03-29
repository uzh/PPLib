package ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation

import ch.uzh.ifi.pdeboer.pplib.process.recombination.{ResultWithCostfunction, SurfaceStructure}

import scala.util.Random
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
/**
  * Created by pdeboer on 27/07/15.
  */
class NaiveAutoExperimentationEngine[INPUT, OUTPUT <: ResultWithCostfunction](surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]]) extends AutoExperimentationEngine[INPUT, OUTPUT](surfaceStructures) {
	override def runOneIteration(input: INPUT): ExperimentResult = {
		val shuffledStructures = surfaceStructures.map(s => (Random.nextDouble(), s)).sortBy(_._1).map(_._2)
		val results = shuffledStructures.mpar.map(s => new SurfaceStructureResult(s, s.test(input)))
		new ExperimentResult(List(ExperimentIteration(results.toList)))
	}
}
