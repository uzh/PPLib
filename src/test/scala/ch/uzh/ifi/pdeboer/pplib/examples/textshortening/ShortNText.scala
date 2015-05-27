package ch.uzh.ifi.pdeboer.pplib.examples.textshortening

import ch.uzh.ifi.pdeboer.pplib.process.recombination.Recombinator

/**
 * Created by pdeboer on 12/05/15.
 */
object ShortNText extends App {
	val testData = new ShortNTestDataInitializer()
	testData.initializePortal()

	val surfaceStructure = new ShortNSurfaceStructure(testData.text)
	val results = new Recombinator(surfaceStructure).recombine().map(variant => {
		(variant, surfaceStructure.runRecombinedVariant(variant))
	})

	println(s"shortest result: ${results.minBy(_._2.length)}")

	println("all results")
	println(results)
}
