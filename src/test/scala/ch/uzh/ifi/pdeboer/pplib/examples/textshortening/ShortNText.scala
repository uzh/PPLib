package ch.uzh.ifi.pdeboer.pplib.examples.textshortening

import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{RecombinationHints, RecombinationVariantGenerator, Recombinator}

/**
 * Created by pdeboer on 12/05/15.
 */
object ShortNText extends App {
	val testData = new ShortNTestDataInitializer()
	testData.initializePortal()

	val recombinator = new Recombinator(RecombinationHints.create(Map(
		RecombinationHints.DEFAULT_HINTS -> {
			RecombinationHints.hcompPlatform(List(testData.hcompPortalWithMockAnswers)) :::
				RecombinationHints.instructions(List(
					new InstructionData(actionName = "shorten the following paragraph", detailedDescription = "grammar (e.g. tenses), text-length")))
		})
	))

	val processes = Map("shortener" -> recombinator.materialize[CreateProcess[_ <: List[Patch], _ <: List[Patch]]])

	val surfaceStructure = new ShortNSurfaceStructure(testData.text)
	val results = new RecombinationVariantGenerator(processes).variants.map(variant => {
		(variant, surfaceStructure.runRecombinedVariant(variant))
	})

	println(results)
}
