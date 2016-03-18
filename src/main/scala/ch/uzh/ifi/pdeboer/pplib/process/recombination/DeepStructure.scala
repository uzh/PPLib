package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

/**
 * Created by pdeboer on 09/10/14.
 */
trait DeepStructure[INPUT, OUTPUT <: ResultWithCostfunction] {
	def run(data: INPUT, recombinedProcessBlueprint: RecombinedProcessBlueprints): OUTPUT

	def defineRecombinationSearchSpace: Map[String, RecombinationSearchSpaceDefinition[_]]
}

trait SimpleDeepStructure[INPUT, OUTPUT <: ResultWithCostfunction] extends DeepStructure[INPUT, OUTPUT] {

	import SimpleDeepStructure._

	def defineSimpleRecombinationSearchSpace: RecombinationSearchSpaceDefinition[_ <: ProcessStub[_, _]]

	override def defineRecombinationSearchSpace: Map[String, RecombinationSearchSpaceDefinition[_]] = {
		Map(DEFAULT_KEY -> defineSimpleRecombinationSearchSpace)
	}
}

object SimpleDeepStructure {
	val DEFAULT_KEY: String = ""
}

class SurfaceStructure[INPUT, OUTPUT <: ResultWithCostfunction](val deepStructure: DeepStructure[INPUT, OUTPUT], val recombinedProcessBlueprint: RecombinedProcessBlueprints) extends LazyLogger {
	def test(data: INPUT): Option[OUTPUT] = try {
		Some(deepStructure.run(data, recombinedProcessBlueprint))
	}
	catch {
		case e: Exception => {
			logger.error(s"An error occurred when testing the deep structure with recombined process $recombinedProcessBlueprint and data $data", e)
			None
		}
	}
}