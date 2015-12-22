package ch.uzh.ifi.pdeboer.pplib.examples.optimization

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.process.entities.{XMLFeatureExpander, SurfaceStructureFeatureExpander}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{AutoExperimentationEngine, Recombinator}

import scala.io.Source

object MCOptimizeConstants {
	val multipeChoiceAnswers = "10,10,10,70"

	lazy val bestAnswer = multipeChoiceAnswers.split(",").map(_.toInt).max

	def answerDistance(answer: Int) = bestAnswer - answer
}

/**
  * Created by pdeboer on 12/05/15.
  * sbt "run-main ch.uzh.ifi.pdeboer.pplib.examples.optimization.MCOptimize"
  */
object MCOptimize extends App {
	val deepStructure = new MCOptimizationDeepStructure()

	val recombinations = new Recombinator(deepStructure).recombine()
	println(s"generated ${recombinations.size} recombinations. running evaluation..")

	val expander = new SurfaceStructureFeatureExpander(recombinations)
	val targetFeatures = expander.featuresInclClass.filter(f => List("TypeTag[Int]", "TypeTag[Double]", XMLFeatureExpander.baseClassFeature.typeName).contains(f.typeName)).toList
	if (args.length == 0) {
		expander.toCSV("optimizationTest.csv", targetFeatures)
		new SpearmintConfigExporter(expander).storeAsJson(new File("/Users/pdeboer/Documents/phd_local/Spearmint/examples/noisyPPLib/config.json"), targetFeatures)
	} else if (args.head == "runall") {
		val autoExperimentationEngine = new AutoExperimentationEngine(recombinations)
		val medians = autoExperimentationEngine.run(MCOptimizeConstants.multipeChoiceAnswers, 19).medianResults
		val medianMap = medians.map(m => m.surfaceStructure -> List(m.result.getOrElse({
			throw new IllegalStateException("no result for m")
			???
		}).doubleRating)).toMap
		expander.toCSV(s"optimizationTestResults${if (args.length == 2) args(1) else ""}.csv", targetFeatures, medianMap)

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
