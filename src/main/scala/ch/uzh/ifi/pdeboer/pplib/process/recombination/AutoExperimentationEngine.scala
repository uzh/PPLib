package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils
import CollectionUtils._

/**
 * Created by pdeboer on 27/07/15.
 */
class AutoExperimentationEngine[INPUT, OUTPUT <: Comparable[OUTPUT]](val surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]]) {
	def runOneIteration(input: INPUT) = {
		val results = surfaceStructures.mpar.map(s => ExperimentResult(s, s.test(input)))
		ExperimentIteration(results.toList)
	}

	def run(input: INPUT, iterations: Int = 1) = {
		(0 to iterations).map(iteration => {
			runOneIteration(input)
		}).toList
	}

	case class ExperimentIteration(results: List[ExperimentResult]) {
		def bestProcess = results.maxBy(_.result)
	}

	case class ExperimentResult(surfaceStructure: SurfaceStructure[INPUT, OUTPUT], result: Option[OUTPUT])

}
