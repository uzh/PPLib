package ch.uzh.ifi.pdeboer.pplib.examples.recombination.translation

import ch.uzh.ifi.pdeboer.pplib.recombination.{Recombinable, RecombinationVariant}
import TranslationProcess._

/**
 * Created by pdeboer on 28/11/14.
 */
class TranslationProcess(val textToImprove: String) extends Recombinable[String] {
	/*
	(original from Patrick's paper)
	It starts by iteratively splitting the input—an article—into paragraphs and then
	sentences (Divide). Then, the resulting sentences are processed in parallel by
	sequentially applying machine translation (MT) and crowd-based rewriting (Rewrite).
	Then the translated sentences are aggregated to paragraphs (Aggre- gate) that are then
	assigned to crowd-workers to improve the language quality by enhancing paragraph
	transitions and en- forcing a consistent wording (Improve Language Quality).
	Finally, the grammatical correctness is improved by elim- inating syntactical and
	grammatical errors (Check Syntax).
	 */
	override def runRecombinedVariant(v: RecombinationVariant): String = {
		val processes = new ProcessVariant(v)

		val paragraphs = textToImprove.split("\\n")

		val rewrittenParagraphs = paragraphs.map(p => {
			val sentences = p.split("\\.").toList
			val rewrittenSentences = processes.rewrite.process(sentences)
			rewrittenSentences.mkString(".")
		}).toList
		val improvedParagraphs = processes.languageQuality.process(rewrittenParagraphs)
		val syntaxImprovedParagraphs = processes.syntaxCheck.process(improvedParagraphs)

		val res = syntaxImprovedParagraphs.mkString("\n")
		res
	}

	override def allRecombinationKeys: List[String] = List(REWRITE, IMPROVE_LANGUAGE_QUALITY, CHECK_SYNTAX)

	private class ProcessVariant(decorated: RecombinationVariant) extends RecombinationVariant(decorated.stubs) {
		def rewrite = decorated.getProcess[List[String], List[String]](REWRITE)

		def languageQuality = decorated.getProcess[List[String], List[String]](IMPROVE_LANGUAGE_QUALITY)

		def syntaxCheck = decorated.getProcess[List[String], List[String]](CHECK_SYNTAX)
	}

}

object TranslationProcess {
	val REWRITE = "1 rewrite"
	val IMPROVE_LANGUAGE_QUALITY = "2 improve language quality"
	val CHECK_SYNTAX = "3 check syntax"
}