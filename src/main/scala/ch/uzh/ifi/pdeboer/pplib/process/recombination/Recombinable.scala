package ch.uzh.ifi.pdeboer.pplib.process.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
trait Recombinable[T] {
	def run(processes: ProcessDeepStructure): T

	def defineRecombinationSearchSpace: Map[String, RecombinationSearchSpaceDefinition[_]]
}


