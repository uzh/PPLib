package ch.uzh.ifi.pdeboer.pplib.examples.optimizationSimulation

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.recombination._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.{ContestWithStatisticalReductionProcess, ContestWithBeatByKVotingProcess, FixPatchProcess}

/**
  * Created by pdeboer on 12/05/15.
  */
case class MCOptimizationResult(text: String, costInCents: Int) extends Comparable[MCOptimizationResult] with ResultWithCostfunction {
	override def compareTo(o: MCOptimizationResult): Int = costFunctionResult.compareTo(o.costFunctionResult)

	override def costFunctionResult: Double = MCOptimizeConstants.answerDistance(text.toInt) + costInCents.toDouble
}

class MCOptimizationDeepStructure extends SimpleDeepStructure[String, MCOptimizationResult] {
	override def run(data: String, blueprint: RecombinedProcessBlueprint): MCOptimizationResult = {
		val options: List[IndexedPatch] = IndexedPatch.from(data, ",")

		type inputType = List[Patch]
		type outputType = Patch

		//create an instance of the recombined process that's currently evaluated
		val generatedShorteningProcess = blueprint.createProcess[inputType, outputType](forcedParams = Map(FixPatchProcess.ALL_DATA.key -> options))

		//run this process and get resulting, shortened text
		val result: Patch = generatedShorteningProcess.process(options)

		//return the result
		MCOptimizationResult(result.value, generatedShorteningProcess match {
			case x: HCompPortalAccess => x.portal.cost
			case _ => throw new IllegalArgumentException("this only works for hcomp portals"); 0
		})
	}

	val HCOMP_PORTAL_TO_USE: HCompPortalAdapter = new MCOptimizationMockPortal()

	override def defineSimpleRecombinationSearchSpace: RecombinationSearchSpaceDefinition[_ <: ProcessStub[_, _]] =
		RecombinationSearchSpaceDefinition[DecideProcess[_ <: List[Patch], _ <: Patch]](
			RecombinationHints.create(Map(
				RecombinationHints.DEFAULT_HINTS -> {
					new AddedParameterRecombinationHint[Int](DefaultParameters.MAX_ITERATIONS, 20 to 30) ::
						new AddedParameterRecombinationHint[Int](DefaultParameters.WORKER_COUNT, 1 to 10) ::
						new AddedParameterRecombinationHint[Int](ContestWithBeatByKVotingProcess.K, 1 to 10) ::
						new AddedParameterRecombinationHint[Double](ContestWithStatisticalReductionProcess.CONFIDENCE_PARAMETER, (1 to 8).map(i => 0.6 + (i.toDouble * .05))) ::
						RecombinationHints.hcompPlatform(List(HCOMP_PORTAL_TO_USE)) :::
						RecombinationHints.instructions(List(
							new InstructionData(actionName = "select the element you like most", detailedDescription = "yeah, that one")))
				})
			)
		)
}
