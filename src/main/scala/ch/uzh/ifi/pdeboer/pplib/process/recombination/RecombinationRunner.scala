package ch.uzh.ifi.pdeboer.pplib.process.recombination

/**
 * Created by pdeboer on 09/10/14.
 */

class RecombinationRunner(subject: Recombinable[_], configurations: List[ProcessCandidate]) {
	lazy val results = {
		configurations.map(c => {
			//TODO: more metrics

			val before = System.currentTimeMillis()
			subject.runRecombinedVariant(c)
			RecombinationResult(c, System.currentTimeMillis() - before)
		})
	}
}

case class RecombinationResult(variant: ProcessCandidate, time: Double)