package ch.uzh.ifi.pdeboer.pplib.util

import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scalacache.ScalaCache
import scalacache.guava.GuavaCache

/**
 * Created by pdeboer on 03/11/14.
 * Based on Marc Tobler's adaption of the MonteCarlo Algorithm used in
 * Automan. Check out their awesome project here: https://github.com/dbarowy/AutoMan
 */
class MonteCarlo {
	private case class Simulation(choices: Int, trials: Int, confidence: Double, iterations: Int, occurrences: ArrayBuffer[Int], var done: Boolean)

	private var simulation: Option[Simulation] = None

	/**
	 * Simulate a number of runs using an RNG
	 * @param choices - number of choices in the multiple choice question
	 * @param trials - number of simultaneous tries (HITs) for each run
	 * @param confidence - the required confidence level
	 * @param iterations - the number of runs to simulate
	 */
	def simulate(choices: Int, trials: Int, confidence: Double, iterations: Int) {
		simulation = Some(Simulation(choices, trials, confidence, iterations, ArrayBuffer.fill(trials + 1)(0), false))
		for (a <- 1 to iterations) {
			iterate(choices, trials)
		}
	}

	/**
	 * Execute one run of the simulation for a multiple choice question
	 * @param numOfChoices - the number of choices of the question
	 * @param trials - how many times to answer the question (number of simultaneous HITs to simulate)
	 * @return - the maximum amount of votes that any answer got
	 */
	private def iterate(numOfChoices: Int, trials: Int): Int = {
		var max = 0
		if (trials > 0) {
			//make a choice for each trials
			val choices = List.fill(trials)(Math.abs(Random.nextInt(Integer.MAX_VALUE)) % numOfChoices)
			//make a histogram
			val hist = choices.map { elem => (elem, choices.count(_ == elem))}.distinct
			//get maximum amount of votes for a single choice
			max = hist.max(Ordering[Int].on[(_, Int)](_._2))._2
		}
		//store information about the maximum amount for this run and return it
		simulation.get.occurrences(max) += 1
		simulation.get.done = true
		max
	}

	/**
	 * Determine the number of trials in order for the odds
	 * to drop below alpha (i.e., 1 - confidence).
	 * This is done by subtracting the area under the histogram for each trial
	 * from 1.0 until the answer is less than alpha
	 * @return the minimum number of workers that have to agree, or None if the suggest number of workers don't suffice
	 */
	private def calculateRequiredAgreement: Option[Int] = {
		require(simulation.isDefined && simulation.get.done)
		val sim = simulation.get
		var i = 1
		var odds = 1.0
		val alpha = 1.0 - sim.confidence

		while ((i <= sim.trials) && (odds > alpha)) {
			val odds_i = sim.occurrences(i) / sim.iterations.toDouble
			odds -= odds_i
			i += 1
		}
		// If we found an answer, then return # of trials
		if ((i <= sim.trials) && (odds <= alpha)) {
			Some(i)
			// Otherwise
		} else {
			// Error condition: not enough trials to achieve the desired confidence.
			None
		}
	}
}

object MonteCarlo {

	import scalacache.memoization._

	implicit val scalaCache = ScalaCache(GuavaCache())

	/**
	 * Calculates the minimum number of tasks the schedule to allow for the provided confidence level to result
	 * @param choices - the number of choice for the multiplce choice question
	 * @param completed_trials - the number of tasks completed so far
	 * @param confidence - the required confidence level
	 * @param iterations - how often to repeat the simulation of the outcome for an estimation
	 * @param max_agreement - the maximum agreement among the already completed trials (nominally)
	 * @return
	 */
	def minTasksRequired(choices: Int, completed_trials: Int, confidence: Double, iterations: Int, max_agreement: Int): Int = memoize {
		var to_run = 0
		var done = false
		val mc = new MonteCarlo
		while (!done) {
			mc.simulate(choices, completed_trials + to_run, confidence, iterations)
			val min_agreement = mc.calculateRequiredAgreement
			val expected = max_agreement + to_run
			if (min_agreement.isEmpty || min_agreement.get > expected) {
				to_run += 1
			} else {
				done = true
			}
		}
		to_run
	}

	/**
	 * Calculates the minimum number workers that have to agree for the provided confidence level
	 * @param choices - the number of choice for the multiple choice question
	 * @param completed_trials - the number of tasks completed so far
	 * @param confidence - the required confidence level
	 * @param iterations - how often to repeat the simulation of the outcome for an estimation
	 * @return the minimum number of workers that have to agree, or None if the completed number of trials are not enough suffice
	 */
	def minAgreementRequired(choices: Int, completed_trials: Int, confidence: Double, iterations: Int): Option[Int] = memoize {
		val mc = new MonteCarlo
		mc.simulate(choices, completed_trials, confidence, iterations)
		mc.calculateRequiredAgreement
	}

}