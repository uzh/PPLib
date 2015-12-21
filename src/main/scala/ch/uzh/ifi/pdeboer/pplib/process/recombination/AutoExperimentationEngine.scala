package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils
import CollectionUtils._

/**
  * Created by pdeboer on 27/07/15.
  */
class AutoExperimentationEngine[INPUT, OUTPUT <: Comparable[OUTPUT]](val surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]]) {
	def runOneIteration(input: INPUT) = {
		val results = surfaceStructures.mpar.map(s => SurfaceStructureResult(s, s.test(input)))
		ExperimentIteration(results.toList)
	}

	def run(input: INPUT, iterations: Int = 1) = {
		val iterationResults = (0 to iterations).map(iteration => {
			runOneIteration(input)
		}).toList
		new ExperimentResult(iterationResults)
	}

	case class ExperimentIteration(rawResults: List[SurfaceStructureResult]) {
		def bestProcess = rawResults.maxBy(_.result)
	}

	case class ExperimentResult(iterations: List[ExperimentIteration]) {
		def resultsForSurfaceStructure(surfaceStructure: SurfaceStructure[INPUT, OUTPUT]) = iterations.map(it => it.rawResults.filter(_.surfaceStructure == surfaceStructure).head)

		def surfaceStructures = iterations.head.rawResults.map(_.surfaceStructure)

		lazy val medianResults = surfaceStructures.map(ss => SurfaceStructureResult(ss, CompositeExperimentResult.medianResult(resultsForSurfaceStructure(ss).map(_.result))))
	}

	case class SurfaceStructureResult(surfaceStructure: SurfaceStructure[INPUT, OUTPUT], result: Option[OUTPUT])

	object CompositeExperimentResult {
		def medianResult(results: List[Option[OUTPUT]]): Option[OUTPUT] = {
			val (lower, upper) = results.sorted.splitAt(results.size / 2)
			upper.head
		}


	}

}
