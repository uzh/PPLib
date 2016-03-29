package ch.uzh.ifi.pdeboer.pplib.examples.optimizationSimulation

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation.BOAutoExperimentationEngine
import ch.uzh.ifi.pdeboer.pplib.process.recombination.Recombinator

object MCOptimizeConstants {
	var multipeChoiceAnswers = ""

	def bestAnswer = multipeChoiceAnswers.split(",").map(_.toInt).max

	def answerDistance(answer: Int) = bestAnswer - answer
}
/**
  * Created by pdeboer on 17/03/16.
  */
object MCOptimizationSimulation extends App {
	val mcAnswersParam = args.find(_.startsWith("answers")).getOrElse("answers10,10,10,70").substring("answers".length)
	MCOptimizeConstants.multipeChoiceAnswers = mcAnswersParam

	val deepStructure = new MCOptimizationDeepStructure()

	val recombinations = new Recombinator(deepStructure).recombine
	println(s"generated ${recombinations.size} recombinations. running evaluation..")

	val autoExperimenter = new BOAutoExperimentationEngine(recombinations, new File("/Users/pdeboer/Documents/phd_local/Spearmint"), "testNewBO")
	val res = autoExperimenter.runOneIteration(MCOptimizeConstants.multipeChoiceAnswers)
	println(res.bestProcess)
}