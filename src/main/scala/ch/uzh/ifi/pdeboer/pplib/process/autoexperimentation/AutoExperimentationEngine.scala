package ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation

import ch.uzh.ifi.pdeboer.pplib.process.recombination.{ResultWithCostfunction, SurfaceStructure}
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, MathUtils}

/**
  * Created by pdeboer on 16/03/16.
  */
abstract class AutoExperimentationEngine[INPUT, OUTPUT <: ResultWithCostfunction](val surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]]) extends LazyLogger {
	def runOneIteration(input: INPUT): ExperimentResult

	def run(input: INPUT, iterations: Int = 1, memoryFriendly: Boolean = false): ExperimentResult = {
		val iterationResults = (1 to iterations).map(iteration => {
			val res = runOneIteration(input)
			if (memoryFriendly)
				res.surfaceStructures.foreach(_.recombinedProcessBlueprint.clear())
			res
		}).toList
		ExperimentResult(iterationResults.flatMap(_.iterations))
	}

	case class ExperimentIteration(rawResults: List[SurfaceStructureResult[INPUT, OUTPUT]]) {
		def bestProcess = rawResults.maxBy(_.result.map(_.costFunctionResult))
	}

	case class ExperimentResult(iterations: List[ExperimentIteration]) {
		def resultsForSurfaceStructure(surfaceStructure: SurfaceStructure[INPUT, OUTPUT]) = iterations.map(it => it.rawResults.find(_.surfaceStructure == surfaceStructure)).filter(_.isDefined).map(_.get)

		def surfaceStructures = resultsOfSuccessfulRuns.map(_.surfaceStructure).toSet

		def rawResults: List[SurfaceStructureResult[INPUT, OUTPUT]] = iterations.flatMap(_.rawResults)

		def resultsOfSuccessfulRuns = rawResults.filter(_.result.isDefined)

		lazy val medianResults = surfaceStructures.map(ss => new SurfaceStructureResult(ss, CompositeExperimentResult.medianResult(resultsForSurfaceStructure(ss).map(_.result))))

		def bestProcess = {
			val groups = surfaceStructures.map(s => s -> resultsForSurfaceStructure(s).filter(_.result.isDefined)).toMap
			val sortedWithMean = groups.map(g => (g._1, MathUtils.mean(g._2.map(_.result.get.costFunctionResult)), MathUtils.stddev(g._2.map(_.result.get.costFunctionResult)))).toList.sortBy(_._2)
			val betterHalfSortedWithMean = sortedWithMean.take(sortedWithMean.size / 2)
			val stdev = MathUtils.stddev(betterHalfSortedWithMean.map(_._2))
			val withinOneStdev = betterHalfSortedWithMean.takeWhile(s => s._2 - s._3 < betterHalfSortedWithMean.head._2 + stdev)
			val bestProcessesWithNonzeroStdev = (sortedWithMean.head :: withinOneStdev).filter(_._3 > 0)
			val winningProcess = if (bestProcessesWithNonzeroStdev.nonEmpty) bestProcessesWithNonzeroStdev.minBy(_._3) else sortedWithMean.head
			groups(winningProcess._1).minBy(_.result.get.costFunctionResult)
		}
	}

	object CompositeExperimentResult {
		def medianResult(results: List[Option[OUTPUT]]): Option[OUTPUT] = {
			val (lower, upper) = results.sortBy(_.map(_.costFunctionResult)).splitAt(results.size / 2)
			upper.head
		}
	}

}

class SurfaceStructureResult[INPUT, OUTPUT <: ResultWithCostfunction](val surfaceStructure: SurfaceStructure[INPUT, OUTPUT], val result: Option[OUTPUT]) {

	def canEqual(other: Any): Boolean = other.isInstanceOf[SurfaceStructureResult[_, _]]

	override def equals(other: Any): Boolean = other match {
		case that: SurfaceStructureResult[_, _] =>
			(that canEqual this) &&
				surfaceStructure == that.surfaceStructure &&
				result == that.result
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(surfaceStructure, result)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}


	override def toString = s"SurfaceStructureResult($surfaceStructure, $result)"
}