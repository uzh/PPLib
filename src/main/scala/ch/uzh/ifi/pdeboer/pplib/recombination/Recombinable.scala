package ch.uzh.ifi.pdeboer.pplib.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
trait Recombinable[T] {
	def runRecombinedVariant(processes: RecombinationVariant): T

	def allRecombinationKeys: List[String]
}


