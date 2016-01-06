package ch.uzh.ifi.pdeboer.pplib.examples.optimization

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.process.entities.{XMLFeatureExpander, SurfaceStructureFeatureExpander}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{AutoExperimentationEngine, Recombinator}

import scala.io.Source

object MCOptimizeConstants {
	var multipeChoiceAnswers = ""

	lazy val bestAnswer = multipeChoiceAnswers.split(",").map(_.toInt).max

	def answerDistance(answer: Int) = bestAnswer - answer
}

/**
  * Created by pdeboer on 12/05/15.
  * sbt "run-main ch.uzh.ifi.pdeboer.pplib.examples.optimization.MCOptimize"
  */
object MCOptimize extends App {
	val mcAnswersParam = args.find(_.startsWith("answers")).getOrElse("answers10,10,10,70").substring("answers".length)
	MCOptimizeConstants.multipeChoiceAnswers = mcAnswersParam

	val deepStructure = new MCOptimizationDeepStructure()

	val recombinations = new Recombinator(deepStructure).recombine()
	println(s"generated ${recombinations.size} recombinations. running evaluation..")

	val expander = new SurfaceStructureFeatureExpander(recombinations)
	val targetFeatures = expander.featuresInclClass.filter(f => List("TypeTag[Int]", "TypeTag[Double]", XMLFeatureExpander.baseClassFeature.typeName).contains(f.typeName)).toList
	if (args.contains("spearmintconfig")) {
		expander.toCSV("optimizationTest.csv", targetFeatures)
		new SpearmintConfigExporter(expander).storeAsJson(new File("/Users/pdeboer/Documents/phd_local/Spearmint/examples/noisyPPLib/config.json"), targetFeatures)
	} else if (args.contains("runall")) {
		val autoExperimentationEngine = new AutoExperimentationEngine(recombinations)
		val results = autoExperimentationEngine.run(MCOptimizeConstants.multipeChoiceAnswers, 20)
		val resultMap = results.surfaceStructures.map(ss => ss -> results.resultsForSurfaceStructure(ss).map(r => r.result.get.doubleRating)).toMap
		expander.toCSV(s"optimizationTestResults$mcAnswersParam.csv", targetFeatures, resultMap)
	} else {
		val featureDefinition = Source.fromFile(args(0)).getLines().map(l => {
			val content = l.split(" VALUE ")
			val valueAsOption = if (content(1) == "None") None else Some(content(1))
			expander.featureByPath(content(0)).get -> valueAsOption
		}).toMap

		val targetSurfaceStructures = expander.findSurfaceStructures(featureDefinition, exactMatch = false)

		assert(targetSurfaceStructures.size <= 1)

		if (targetSurfaceStructures.isEmpty) println(10000.0)
		else {
			val autoExperimentation = new AutoExperimentationEngine(targetSurfaceStructures)
			val results = autoExperimentation.runOneIteration(MCOptimizeConstants.multipeChoiceAnswers)

			println(results.rawResults.head.result.get.doubleRating)
		}
	}
}
