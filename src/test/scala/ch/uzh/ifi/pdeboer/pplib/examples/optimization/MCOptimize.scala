package ch.uzh.ifi.pdeboer.pplib.examples.optimization

import ch.uzh.ifi.pdeboer.pplib.process.entities.SurfaceStructureFeatureExpander
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{AutoExperimentationEngine, Recombinator}

/**
  * Created by pdeboer on 12/05/15.
  */
object MCOptimize extends App {
	val deepStructure = new MCOptimizationDeepStructure()

	val recombinations = new Recombinator(deepStructure).recombine()
	println(s"generated ${recombinations.size} recombinations. running evaluation..")

	val multipeChoiceAnswers = "10,10,10,70"

	private val expander = new SurfaceStructureFeatureExpander(recombinations)
	private val targetFeatures = expander.featuresInclClass.filter(f => List("TypeTag[Int]", "TypeTag[Double]", expander.baseClassFeature.typeName).contains(f.typeName)).toList
	expander.toCSV("optimizationTest.csv", targetFeatures)

	val autoExperimentation = new AutoExperimentationEngine(recombinations)
	val results = autoExperimentation.runOneIteration(multipeChoiceAnswers)

	println("finished evaluation.")
	println(s"best result: ${results.bestProcess}")
}
