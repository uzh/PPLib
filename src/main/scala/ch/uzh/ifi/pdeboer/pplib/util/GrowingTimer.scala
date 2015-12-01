package ch.uzh.ifi.pdeboer.pplib.util

import scala.concurrent.duration.Duration

/**
 * Utility class that acts as a timer for the caller. Timer Duration grows on each call received.
 * @param start the initial time duration (in ms)
 * @param factor the factor by which the time increases on each call
 * @param max the maximum duration of the timeout (in ms)
 */
class GrowingTimer(val start: Duration, val factor: Double, val max: Duration) extends LazyLogger {
	var currentTime = start

	/**
	 * Waits the current amount of time
	 */
	def waitTime = {
		try {
			Thread.sleep(currentTime.toMillis)
		} catch {
			case e: InterruptedException => {
				logger.info("Parked thread got woken up. Let's see what's going on..")
				Thread.interrupted() //reset interruption status
			}
		}
		updateTimer
	}

	private def updateTimer = {
		currentTime *= factor
		if (currentTime > max)
			currentTime = max
	}
}
