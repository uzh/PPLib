package ch.uzh.ifi.pdeboer.pplib.patterns.pruners

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompAnswer

/**
 * Created by pdeboer on 12/12/14.
 */
trait Pruner {
	def prune(answers: List[HCompAnswer]): List[HCompAnswer]
}

class SigmaPruner(numSigmas: Int) extends Pruner {
	override def prune(answers: List[HCompAnswer]): List[HCompAnswer] = {
		val sigmaCalc = new SigmaCalculator(
			answers.map(_.processingTimeMillis.toDouble).toList, numSigmas)

		answers.filter(a => {
			val procTime: Double = a.processingTimeMillis.toDouble
			procTime >= sigmaCalc.minAllowedValue && procTime <= sigmaCalc.maxAllowedValue
		})
	}
}