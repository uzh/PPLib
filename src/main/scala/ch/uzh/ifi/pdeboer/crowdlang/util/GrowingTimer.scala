package ch.uzh.ifi.pdeboer.crowdlang.util

import scala.concurrent.duration.Duration

/**
 * Utility class that acts as a timer for the caller. Timer Duration grows on each call received.
 * @param start the initial time duration (in ms)
 * @param factor the factor by which the time increases on each call
 * @param max the maximum duration of the timeout (in ms)
 */
class GrowingTimer(val start: Duration, val factor: Double, val max: Duration) {
	var currentTime = start

	/**
	 * Waits the current amount of time
	 */
	def waitTime = {
		Thread.sleep(currentTime.toMillis)
		updateTimer
	}

	private def updateTimer = {
		currentTime *= factor
		if (currentTime > max)
			currentTime = max
	}
}
