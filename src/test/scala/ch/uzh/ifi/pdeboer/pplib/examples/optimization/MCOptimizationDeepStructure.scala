package ch.uzh.ifi.pdeboer.pplib.examples.optimization

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.recombination._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FixPatchProcess

/**
  * Created by pdeboer on 12/05/15.
  */
case class MCOptimizationResult(text: String, costInCents: Int) extends Comparable[MCOptimizationResult] {
	override def compareTo(o: MCOptimizationResult): Int = -1 * text.length.compareTo(o.text.length)
}

class MCOptimizationDeepStructure extends SimpleDeepStructure[String, MCOptimizationResult] {
	override def run(data: String, blueprint: RecombinedProcessBlueprints): MCOptimizationResult = {
		val options: List[IndexedPatch] = IndexedPatch.from(data, ",")

		type inputType = List[Patch]
		type outputType = Patch

		//create an instance of the recombined process that's currently evaluated
		val generatedShorteningProcess = blueprint.createProcess[inputType, outputType](forcedParams = Map(FixPatchProcess.ALL_DATA.key -> options))

		//run this process and get resulting, shortened text
		val result: Patch = generatedShorteningProcess.process(options)

		//return the result
		MCOptimizationResult(result.value, generatedShorteningProcess.costSoFar)
	}

	val HCOMP_PORTAL_TO_USE: HCompPortalAdapter = new MCOptimizationMockPortal()

	override def defineSimpleRecombinationSearchSpace: RecombinationSearchSpaceDefinition[_ <: ProcessStub[_, _]] =
		RecombinationSearchSpaceDefinition[DecideProcess[_ <: List[Patch], _ <: Patch]](
			RecombinationHints.create(Map(
				RecombinationHints.DEFAULT_HINTS -> {
					RecombinationHints.hcompPlatform(List(HCOMP_PORTAL_TO_USE)) :::
						RecombinationHints.instructions(List(
							new InstructionData(actionName = "select the element you like most", detailedDescription = "yeah, that one")))
				})
			)
		)
}
