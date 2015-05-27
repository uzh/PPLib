package ch.uzh.ifi.pdeboer.pplib.process.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
trait Recombinable[T] {
	def runRecombinedVariant(processes: RecombinationVariant): T

	def requiredProcessDefinitions: Map[String, RecombinationProcessDefinition[_]]
}


