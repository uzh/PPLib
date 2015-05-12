package ch.uzh.ifi.pdeboer.pplib.examples.textshortening

import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{RecombinationHints, RecombinationVariantGenerator, Recombinator}

/**
 * Created by pdeboer on 12/05/15.
 */
object ShortNText extends App {
	val recombinator = new Recombinator(RecombinationHints.create(Map(
		RecombinationHints.DEFAULT_HINTS -> {
			RecombinationHints.hcompPlatform(List(HComp.randomPortal)) :::
				RecombinationHints.instructions(List(
					new InstructionData(actionName = "shorten the following paragraph", detailedDescription = "grammar (e.g. tenses), text-length")))
		})
	))

	val processes = Map("shortener" -> recombinator.materialize[CreateProcess[_ <: List[Patch], _ <: List[Patch]]])

	val surfaceStructure = new ShortNSurfaceStructure("This text is way too long and could be shortened by anyone except for people who can't")
	val results = new RecombinationVariantGenerator(processes).variants.map(variant => {
		(variant, surfaceStructure.runRecombinedVariant(variant))
	})

	println(results)
}
