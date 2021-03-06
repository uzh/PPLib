package ch.uzh.ifi.pdeboer.pplib.util

import org.joda.time.DateTime
import org.junit.Assert

import scala.util.Random

/**
 * Created by pdeboer on 24/07/15.
 */
class CollectionUtilTest {
	//@Test
	def testManyParallelThreads: Unit = {
		val mparTime: Long = runMPar
		val parTime: Long = runPar
		val mparRuntime: Long = mparTime
		val delta = parTime - mparTime

		Assert.assertTrue(s"delta was $delta. Tolerance would be $mparRuntime", delta > mparRuntime)
	}

	def runMPar: Long = {
		import CollectionUtils._
		val timeAtStart = DateTime.now()
		val numCores = Runtime.getRuntime.availableProcessors()
		val totalWaitingIterations: Int = numCores * 10
		val waitingTime: Int = 500
		val blocking = (1 to totalWaitingIterations).mpar.map(c => {
			Thread.sleep(waitingTime)
			Random.nextDouble()
		})
		DateTime.now().getMillis - timeAtStart.getMillis
	}

	def runPar: Long = {
		val timeAtStart = DateTime.now()
		val numCores = Runtime.getRuntime.availableProcessors()
		val totalWaitingIterations: Int = numCores * 10
		val waitingTime: Int = 500
		val blocking = (1 to totalWaitingIterations).par.map(c => {
			Thread.sleep(waitingTime)
			Random.nextDouble()
		})
		DateTime.now().getMillis - timeAtStart.getMillis
	}
}
