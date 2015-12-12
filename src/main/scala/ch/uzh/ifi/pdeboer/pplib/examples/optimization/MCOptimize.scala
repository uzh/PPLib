package ch.uzh.ifi.pdeboer.pplib.examples.optimization

import ch.uzh.ifi.pdeboer.pplib.process.entities.SurfaceStructureFeatureExpander
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{AutoExperimentationEngine, Recombinator}

import scala.io.Source

object MCOptimizeConstants {
	val multipeChoiceAnswers = "10,10,10,70"

	lazy val bestAnswer = multipeChoiceAnswers.split(",").map(_.toInt).max

	def answerDistance(answer: Int) = bestAnswer - answer
}

/**
  * Created by pdeboer on 12/05/15.
  */
object MCOptimize extends App {
	val deepStructure = new MCOptimizationDeepStructure()

	val recombinations = new Recombinator(deepStructure).recombine()
	println(s"generated ${recombinations.size} recombinations. running evaluation..")

	val expander = new SurfaceStructureFeatureExpander(recombinations)
	if (args.length == 0) {
		val targetFeatures = expander.featuresInclClass.filter(f => List("TypeTag[Int]", "TypeTag[Double]", expander.baseClassFeature.typeName).contains(f.typeName)).toList
		expander.toCSV("optimizationTest.csv", targetFeatures)
	} else if (args.length == 1) {
		val featureDefinition = Source.fromFile(args(0)).getLines().map(l => {
			val content = l.split(" VALUE ")
			val valueAsOption = if (content(1) == "None") None else Some(content(1))
			expander.featureByPath(content(0)).get -> valueAsOption
		}).toMap

		val targetSurfaceStructures = expander.findSurfaceStructures(featureDefinition)

		assert(targetSurfaceStructures.size == 1)

		val autoExperimentation = new AutoExperimentationEngine(targetSurfaceStructures)
		val results = autoExperimentation.runOneIteration(MCOptimizeConstants.multipeChoiceAnswers)

		println(results.results.head.result.get.doubleRating)
	}
}
