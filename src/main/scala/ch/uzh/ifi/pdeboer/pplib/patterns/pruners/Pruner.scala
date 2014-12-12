package ch.uzh.ifi.pdeboer.pplib.patterns.pruners

/**
 * Created by pdeboer on 12/12/14.
 */
trait Pruner extends Serializable {
	def prune[P <: Prunable](answers: List[P]): List[P]
}

class NoPruner extends Pruner {
	override def prune[P <: Prunable](answers: List[P]): List[P] = answers
}

class SigmaPruner(numSigmas: Int) extends Pruner {
	override def prune[P <: Prunable](answers: List[P]): List[P] = {
		val sigmaCalc = new SigmaCalculator(
			answers.map(_.prunableDouble).toList, numSigmas)

		answers.filter(a => {
			val prunable: Double = a.prunableDouble
			prunable >= sigmaCalc.minAllowedValue && prunable <= sigmaCalc.maxAllowedValue
		})
	}
}

trait Prunable {
	def prunableDouble: Double
}