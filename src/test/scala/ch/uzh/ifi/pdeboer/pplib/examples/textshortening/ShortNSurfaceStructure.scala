package ch.uzh.ifi.pdeboer.pplib.examples.textshortening

import ch.uzh.ifi.pdeboer.pplib.process.entities.{CreateProcess, IndexedPatch, Patch, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{Recombinable, RecombinationVariant}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FixPatchProcess
import ch.uzh.ifi.pdeboer.pplib.util.StringWrapper

/**
 * Created by pdeboer on 12/05/15.
 */
class ShortNSurfaceStructure(textToBeShortened: String) extends Recombinable[String] {
	override def runRecombinedVariant(generatedRecombinationVariants: RecombinationVariant): String = {
		val paragraphs: List[IndexedPatch] = textToBeShortened.split("\n").zipWithIndex.map(p => new IndexedPatch(p._1, p._2, Some(StringWrapper(p._1)))).toList

		val generatedShorteningProcess = generatedRecombinationVariants.createProcess[List[Patch], List[Patch]](
			"shortener", higherPrioParams = Map(FixPatchProcess.ALL_DATA.key -> paragraphs)) //TODO add syntactic sugar here
		val result = generatedShorteningProcess.process(paragraphs)

		result.mkString("\n")
	}

	override def requiredProcessDefinitions: Map[String, Class[_ <: ProcessStub[_, _]]] =
		Map("shortener" -> classOf[CreateProcess[_ <: List[Patch], _ <: List[Patch]]])
}
