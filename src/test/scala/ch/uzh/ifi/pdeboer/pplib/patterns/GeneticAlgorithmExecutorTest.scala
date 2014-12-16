package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 10/12/14.
 */
class GeneticAlgorithmExecutorTest {
	@Test
	def testMethodsCalled: Unit = {
		val driver: TestDriver = new TestDriver()
		val exec = new GeneticAlgorithmExecutor(driver, new GAIterationLimitTerminator(10))
		println(exec.refinedData)
		Assert.assertTrue(driver.counter > 1)
		Assert.assertEquals(1, exec.refinedData.takeRight(1)(0).toString.length)
		Assert.assertTrue(exec.refinedData(0).toString.length > 1)
	}

	private class TestDriver extends GeneticAlgorithmDriver {
		var counter: Int = 0

		override def initialPopulation: GAPopulation = new GAPopulation(
			List("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
				.map(c => new GAChromosome(new Patch(c), 0))
		)

		override def combine(patch1: Patch, patch2: Patch): Patch = {
			counter += 1
			new Patch("" + patch1 + patch2)
		}

		override def mutate(patch: Patch): Patch = {
			counter += 1
			new Patch(patch + "1")
		}

		override def fitness(patch: Patch): Double = -patch.toString.length
	}

}
