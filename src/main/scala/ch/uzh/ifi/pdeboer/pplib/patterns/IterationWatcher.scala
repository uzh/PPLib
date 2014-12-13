package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.util.U

/**
 * Created by pdeboer on 13/12/14.
 */
class IterationWatcher(val originalText: String, stringDifferenceTerminationThreshold: Int = 1, toleratedNumberOfIterationsBelowThreshold: Int = 2) {
	assert(toleratedNumberOfIterationsBelowThreshold > 0)

	private var iterations = List(originalText)

	def addIteration(iteration: String): Unit = {
		iterations = iteration :: iterations
	}

	def shouldRunAnotherIteration: Boolean = {
		val zeroStart = iterations.take(toleratedNumberOfIterationsBelowThreshold)
		val oneStart = iterations.drop(1).take(toleratedNumberOfIterationsBelowThreshold)

		zeroStart
			.zip(oneStart)
			.map(s => if (U.stringDistance(s._1, s._2) <= stringDifferenceTerminationThreshold) 1 else 0)
			.sum < toleratedNumberOfIterationsBelowThreshold
	}
}
