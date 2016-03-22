package ch.uzh.ifi.pdeboer.pplib.examples.optimizationSimulation

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation.{SpearmintConfigExporter, NaiveAutoExperimentationEngine}
import ch.uzh.ifi.pdeboer.pplib.process.entities.{XMLFeatureExpander, SurfaceStructureFeatureExpander}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.Recombinator

import scala.io.Source

/**
  * Created by pdeboer on 12/05/15.
  * sbt "run-main ch.uzh.ifi.pdeboer.pplib.examples.optimization.MCOptimize"
  */
@deprecated object MCOptimizeExternal extends App {
	val mcAnswersParam = args.find(_.startsWith("answers")).getOrElse("answers10,10,10,70").substring("answers".length)
	MCOptimizeConstants.multipeChoiceAnswers = mcAnswersParam

	val deepStructure = new MCOptimizationDeepStructure()

	val recombinations = new Recombinator(deepStructure).recombine
	println(s"generated ${recombinations.size} recombinations. running evaluation..")

	val expander = new SurfaceStructureFeatureExpander(recombinations)
	val targetFeatures = expander.featuresInclClass.filter(f => List("TypeTag[Int]", "TypeTag[Double]", XMLFeatureExpander.baseClassFeature.typeName).contains(f.typeName)).toList
	if (args.contains("spearmintconfig")) {
		expander.toCSV("optimizationTest.csv", targetFeatures)
		new SpearmintConfigExporter(expander).storeAsJson(new File("/Users/pdeboer/Documents/phd_local/Spearmint/examples/noisyPPLib/config.json"), targetFeatures)
	} else if (args.contains("runall")) {
		val autoExperimentationEngine = new NaiveAutoExperimentationEngine(recombinations)
		val results = autoExperimentationEngine.run(MCOptimizeConstants.multipeChoiceAnswers, 5)
		val resultMap = results.surfaceStructures.map(ss => ss -> {
			val resultsObjects = results.resultsForSurfaceStructure(ss).zipWithIndex
			val resultsUtilities = resultsObjects.map(r => "iteration_utility_" + r._2 -> r._1.result.get.costFunctionResult)
			val resultCost = resultsObjects.map(r => "iteration_cost_" + r._2 -> r._1.result.get.costInCents)

			resultCost ::: resultsUtilities
		}.toMap).toMap
		val hostname = args.find(_.startsWith("hostname")).map(_.substring("hostname".length))
		expander.toCSV(s"optimizationTestResults$mcAnswersParam${hostname.map(h => "_" + h).getOrElse("")}.csv", targetFeatures, resultMap)
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
			val autoExperimentation = new NaiveAutoExperimentationEngine(targetSurfaceStructures)
			val results = autoExperimentation.runOneIteration(MCOptimizeConstants.multipeChoiceAnswers)
			/*
			println("cost was " + results.rawResults.head.result.get.costInCents)

			println(results.rawResults.head.result.get.doubleRating)*/
		}
	}
}
