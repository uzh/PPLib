package ch.uzh.ifi.pdeboer.crowdlang.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
class Recombinator(subject: RecombinatedSubject, configurations: List[RecombinationVariant]) {
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