package ch.uzh.ifi.pdeboer.pplib.examples.textshortening

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompPortalAdapter, HComp}
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{Recombinable, RecombinationHints, RecombinationSearchSpaceDefinition, ProcessCandidate}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FixPatchProcess
import ch.uzh.ifi.pdeboer.pplib.util.StringWrapper

/**
 * Created by pdeboer on 12/05/15.
 */
class ShortNSurfaceStructure(textToBeShortened: String) extends Recombinable[String] {
	override def runProcessCandidate(processBlueprint: ProcessCandidate): String = {
		//split the text to be shortened into it's paragraphs am memorize the index of every paragraph
		val paragraphs: List[IndexedPatch] = textToBeShortened.split("\n").zipWithIndex.map(p => new IndexedPatch(p._1, p._2, Some(StringWrapper(p._1)))).toList

		//create an instance of the recombined process that's currently evaluated
		val generatedShorteningProcess = processBlueprint.createProcess[List[Patch], List[Patch]](
			SHORTENER_PROCESS_KEY, forcedParams = Map(FixPatchProcess.ALL_DATA.key -> paragraphs)
		)

		//run this process and get resulting, shortened text
		val result: List[Patch] = generatedShorteningProcess.process(paragraphs)

		//return the result
		result.mkString("\n")
	}


	override def defineRecombinationSearchSpace: Map[String, RecombinationSearchSpaceDefinition[_]] =
		Map(SHORTENER_PROCESS_KEY -> RecombinationSearchSpaceDefinition[CreateProcess[_ <: List[Patch], _ <: List[Patch]]](
			RecombinationHints.create(Map(
				RecombinationHints.DEFAULT_HINTS -> {
					RecombinationHints.hcompPlatform(List(HCOMP_PORTAL_TO_USE)) :::
						RecombinationHints.instructions(List(
							new InstructionData(actionName = "shorten the following paragraph", detailedDescription = "grammar (e.g. tenses), text-length")))
				})
			)
		))

	val SHORTENER_PROCESS_KEY: String = "shortener"

	val HCOMP_PORTAL_TO_USE: HCompPortalAdapter = HComp(ShortNTestDataInitializer.TEST_PORTAL_KEY)
}
