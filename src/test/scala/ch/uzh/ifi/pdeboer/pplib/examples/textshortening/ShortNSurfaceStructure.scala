package ch.uzh.ifi.pdeboer.pplib.examples.textshortening

import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{Recombinable, RecombinationHints, RecombinationProcessDefinition, RecombinationVariant}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FixPatchProcess
import ch.uzh.ifi.pdeboer.pplib.util.StringWrapper

/**
 * Created by pdeboer on 12/05/15.
 */
class ShortNSurfaceStructure(textToBeShortened: String) extends Recombinable[String] {
	override def runRecombinedVariant(generatedRecombinationVariants: RecombinationVariant): String = {
		val paragraphs: List[IndexedPatch] = textToBeShortened.split("\n").zipWithIndex.map(p => new IndexedPatch(p._1, p._2, Some(StringWrapper(p._1)))).toList
		//generatedRecombinationVariants.stubs.exists(s => new ProcessPrinter(s._2).toString.contains("IterativeRefinement"))
		val generatedShorteningProcess = generatedRecombinationVariants.createProcess[List[Patch], List[Patch]](
			"shortener", higherPrioParams = Map(FixPatchProcess.ALL_DATA.key -> paragraphs)
		)
		val result: List[Patch] = generatedShorteningProcess.process(paragraphs)

		result.mkString("\n")
	}

	override def requiredProcessDefinitions: Map[String, RecombinationProcessDefinition[_]] =
		Map("shortener" -> RecombinationProcessDefinition[CreateProcess[_ <: List[Patch], _ <: List[Patch]]](
			RecombinationHints.create(Map(
				RecombinationHints.DEFAULT_HINTS -> {
					RecombinationHints.hcompPlatform(List(HComp(ShortNTestDataInitializer.TEST_PORTAL_KEY))) :::
						RecombinationHints.instructions(List(
							new InstructionData(actionName = "shorten the following paragraph", detailedDescription = "grammar (e.g. tenses), text-length")))
				})
			)
		))
}
