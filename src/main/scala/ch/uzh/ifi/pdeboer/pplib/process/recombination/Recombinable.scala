package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub

/**
 * Created by pdeboer on 09/10/14.
 */
trait Recombinable[T] {
	def run(processes: ProcessSurfaceStructure): T

	def defineRecombinationSearchSpace: Map[String, RecombinationSearchSpaceDefinition[_]]
}

trait SimpleRecombinable[E] extends Recombinable[E] {

	import SimpleRecombinable._

	def defineSimpleRecombinationSearchSpace: RecombinationSearchSpaceDefinition[_ <: ProcessStub[_, _]]

	override def defineRecombinationSearchSpace: Map[String, RecombinationSearchSpaceDefinition[_]] = {
		Map(DEFAULT_KEY -> defineSimpleRecombinationSearchSpace)
	}
}

object SimpleRecombinable {
	val DEFAULT_KEY: String = ""
}
