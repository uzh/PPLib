package ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation

import ch.uzh.ifi.pdeboer.pplib.process.recombination.{ResultWithUtility, SurfaceStructure}
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, MathUtils}

/**
  * Created by pdeboer on 16/03/16.
  */
abstract class AutoExperimentationEngine[INPUT, OUTPUT <: ResultWithUtility](val surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]]) extends LazyLogger {
	def runOneIteration(input: INPUT): ExperimentResult

	def run(input: INPUT, iterations: Int = 1): ExperimentResult = {
		val iterationResults = (0 to iterations).map(iteration => {
			runOneIteration(input)
		}).toList
		ExperimentResult(iterationResults.flatMap(_.iterations))
	}

	case class ExperimentIteration(rawResults: List[SurfaceStructureResult[INPUT, OUTPUT]]) {
		def bestProcess = rawResults.maxBy(_.result.map(_.utility))
	}

	case class ExperimentResult(iterations: List[ExperimentIteration]) {
		def resultsForSurfaceStructure(surfaceStructure: SurfaceStructure[INPUT, OUTPUT]) = iterations.map(it => it.rawResults.find(_.surfaceStructure == surfaceStructure)).filter(_.isDefined).map(_.get)

		def surfaceStructures = resultsOfSuccessfulRuns.map(_.surfaceStructure).toSet

		def rawResults: List[SurfaceStructureResult[INPUT, OUTPUT]] = iterations.flatMap(_.rawResults)

		def resultsOfSuccessfulRuns = rawResults.filter(_.result.isDefined)

		lazy val medianResults = surfaceStructures.map(ss => SurfaceStructureResult(ss, CompositeExperimentResult.medianResult(resultsForSurfaceStructure(ss).map(_.result))))

		def bestProcess = {
			val stdev = MathUtils.stddev(resultsOfSuccessfulRuns.map(_.result.get.utility))
			val groups = surfaceStructures.map(s => s -> resultsForSurfaceStructure(s)).toMap
			val sortedWithMean = groups.map(g => (g._1, MathUtils.mean(g._2.map(_.result.get.utility)), MathUtils.stddev(g._2.map(_.result.get.utility)))).toList.sortBy(_._2)
			val withinOneStdev = sortedWithMean.takeWhile(s => s._2 - s._3 < sortedWithMean.head._2 + stdev)
			val bestProcessesWithNonzeroStdev = (sortedWithMean.head :: withinOneStdev).filter(_._3 > 0)
			val winningProcess = if (bestProcessesWithNonzeroStdev.nonEmpty) bestProcessesWithNonzeroStdev.minBy(_._3) else sortedWithMean.head
			groups(winningProcess._1).minBy(_.result.get.utility)
		}
	}


	object CompositeExperimentResult {
		def medianResult(results: List[Option[OUTPUT]]): Option[OUTPUT] = {
			val (lower, upper) = results.sortBy(_.map(_.utility)).splitAt(results.size / 2)
			upper.head
		}
	}
}

case class SurfaceStructureResult[INPUT, OUTPUT <: ResultWithUtility](surfaceStructure: SurfaceStructure[INPUT, OUTPUT], result: Option[OUTPUT])

