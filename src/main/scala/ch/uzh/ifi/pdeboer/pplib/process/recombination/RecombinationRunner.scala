package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub

/**
 * Created by pdeboer on 09/10/14.
 */
class Recombinator(baseClass: Class[_ <: ProcessStub[_, _]], defaultParameters: Map[String, Any], db: RecombinationDB) {

}

class RecombinationRunner(subject: Recombinable[_], configurations: List[RecombinationVariant]) {
	lazy val results = {
		configurations.map(c => {
			//TODO: more metrics

			val before = System.currentTimeMillis()
			subject.runRecombinedVariant(c)
			RecombinationResult(c, System.currentTimeMillis() - before)
		})
	}
}

case class RecombinationResult(variant: RecombinationVariant, time: Double)