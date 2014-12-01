package ch.uzh.ifi.pdeboer.pplib.examples.recombination.translation

import ch.uzh.ifi.pdeboer.pplib.recombination.RecombinationVariantXMLExporter

import scala.xml.XML

/**
 * Created by pdeboer on 01/12/14.
 */
object TranslationApp extends App {
	val xml = <Variations>
		{TranslationRecombination.recombinations.map(v => new RecombinationVariantXMLExporter(v).xml)}
	</Variations>

	XML.save("variations.xml", xml)
}
