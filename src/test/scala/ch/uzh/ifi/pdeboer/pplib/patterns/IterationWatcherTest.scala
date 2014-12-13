package ch.uzh.ifi.pdeboer.pplib.patterns

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 13/12/14.
 */
class IterationWatcherTest {
	val string1 = "uiop"

	@Test
	def testIterationsTrivial: Unit = {
		val watcher: IterationWatcher = new IterationWatcher(string1)
		Assert.assertTrue(watcher.shouldRunAnotherIteration)
	}

	@Test
	def testIterationsEqual: Unit = {
		val watcher: IterationWatcher = new IterationWatcher(string1, stringDifferenceTerminationThreshold = 0, numberOfToleratedLowSteps = 1)
		watcher.addIteration(string1)
		Assert.assertFalse(watcher.shouldRunAnotherIteration)
	}

	@Test
	def testIterationsAlmostEqual: Unit = {
		val watcher: IterationWatcher = new IterationWatcher(string1, stringDifferenceTerminationThreshold = 1, numberOfToleratedLowSteps = 1)
		watcher.addIteration(string1 + "a")
		Assert.assertFalse(watcher.shouldRunAnotherIteration)
	}

	@Test
	def testIterationShouldContinue: Unit = {
		val watcher: IterationWatcher = new IterationWatcher(string1, stringDifferenceTerminationThreshold = 0, numberOfToleratedLowSteps = 1)
		watcher.addIteration(string1 + "aasdf")
		Assert.assertTrue(watcher.shouldRunAnotherIteration)
	}

	@Test
	def testMultiIterationStep: Unit = {
		val watcher: IterationWatcher = new IterationWatcher(string1, stringDifferenceTerminationThreshold = 0, numberOfToleratedLowSteps = 1)

		Assert.assertTrue(List("a", "s", "d", "f").forall(c => {
			watcher.addIteration(c)
			watcher.shouldRunAnotherIteration
		}))
	}

	@Test
	def testMultiIterationStepBad: Unit = {
		val watcher: IterationWatcher = new IterationWatcher(string1, stringDifferenceTerminationThreshold = 0, numberOfToleratedLowSteps = 1)

		Assert.assertTrue(List("a", "s", "d", "f").forall(c => {
			watcher.addIteration(c)
			watcher.shouldRunAnotherIteration
		}))
		watcher.addIteration("f")
		Assert.assertFalse(watcher.shouldRunAnotherIteration)
	}


	@Test
	def testMultiIteration2StepBad: Unit = {
		val watcher: IterationWatcher = new IterationWatcher(string1, stringDifferenceTerminationThreshold = 0, numberOfToleratedLowSteps = 2)

		Assert.assertTrue(List("a", "s", "d", "d").forall(c => {
			watcher.addIteration(c)
			watcher.shouldRunAnotherIteration
		}))
		watcher.addIteration("f")
		Assert.assertTrue(watcher.shouldRunAnotherIteration)
		watcher.addIteration("f")
		Assert.assertTrue(watcher.shouldRunAnotherIteration)
		watcher.addIteration("f")
		Assert.assertFalse(watcher.shouldRunAnotherIteration)
	}


	@Test
	def testMultiIteration2StepBadWithStringDistance: Unit = {
		val watcher: IterationWatcher = new IterationWatcher(string1, stringDifferenceTerminationThreshold = 2, numberOfToleratedLowSteps = 2)
		Assert.assertTrue(List("as88", "as99").forall(c => {
			watcher.addIteration(c)
			watcher.shouldRunAnotherIteration
		}))
		watcher.addIteration("af77")
		Assert.assertTrue(watcher.shouldRunAnotherIteration)

		watcher.addIteration("8877")
		Assert.assertTrue(watcher.shouldRunAnotherIteration)

		watcher.addIteration("9875")
		Assert.assertFalse(watcher.shouldRunAnotherIteration)
	}
}
