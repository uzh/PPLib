package ch.uzh.ifi.pdeboer.pplib.examples.textshortening

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.recombination._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FixPatchProcess

/**
 * Created by pdeboer on 12/05/15.
 */
case class ShortNResult(text: String, costInCents: Int, durationInSeconds: Int) extends Comparable[ShortNResult] {
	override def compareTo(o: ShortNResult): Int = -1 * text.length.compareTo(o.text.length)
}

class ShortNDeepStructure extends SimpleDeepStructure[String, ShortNResult] {
	override def run(data: String, blueprint: RecombinedProcessBlueprints): ShortNResult = {
		//split the text to be shortened into it's paragraphs am memorize the index of every paragraph
		val paragraphs: List[IndexedPatch] = IndexedPatch.from(data)

		type inputType = List[Patch]
		type outputType = inputType

		//create an instance of the recombined process that's currently evaluated
		val generatedShorteningProcess = blueprint.createProcess[inputType, outputType](forcedParams = Map(FixPatchProcess.ALL_DATA.key -> paragraphs))

		//run this process and get resulting, shortened text
		val result: List[Patch] = generatedShorteningProcess.process(paragraphs)

		//return the result
		ShortNResult(result.mkString("\n"), generatedShorteningProcess.costSoFar, generatedShorteningProcess.durationSoFar)
	}

	val HCOMP_PORTAL_TO_USE: HCompPortalAdapter = HComp(ShortNTestDataInitializer.TEST_PORTAL_KEY)

	override def defineSimpleRecombinationSearchSpace: RecombinationSearchSpaceDefinition[_ <: ProcessStub[_, _]] =
		RecombinationSearchSpaceDefinition[CreateProcess[_ <: List[Patch], _ <: List[Patch]]](
			RecombinationHints.create(Map(
				RecombinationHints.DEFAULT_HINTS -> {
					RecombinationHints.hcompPlatform(List(HCOMP_PORTAL_TO_USE)) :::
						RecombinationHints.instructions(List(
							new InstructionData(actionName = "shorten the following paragraph", detailedDescription = "grammar (e.g. tenses), text-length")))
				})
			)
		)
}
