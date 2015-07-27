package ch.uzh.ifi.pdeboer.pplib.examples.textshortening

import ch.uzh.ifi.pdeboer.pplib.process.recombination.{AutoExperimentationEngine, Recombinator}

/**
 * Created by pdeboer on 12/05/15.
 */
object ShortNText extends App {
	val testData = new ShortNTestDataInitializer()
	testData.initializePortal()

	val deepStructure = new ShortNDeepStructure()

	val recombinations = new Recombinator(deepStructure).recombine()
	println(s"generated ${recombinations.size} recombinations. running evaluation..")

	val textToBeShortened = """This text is too long and could be shortened by anyone except for people who can't.
		  The 2nd sentence is also very useless.
		  And the third one as well - very much so."""

	val autoExperimentation = new AutoExperimentationEngine(recombinations)
	val results = autoExperimentation.runOneIteration(textToBeShortened)

	println("finished evaluation.")
	println(s"best result: ${results.bestProcess}")
}
