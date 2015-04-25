package ch.uzh.ifi.pdeboer.pplib.process.recombination

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 25/04/15.
 */
class CombinationGeneratorTest {
	@Test
	def testCombinationWithEmptySet: Unit = {
		val res = CombinationGenerator.generate(List(List.empty[Int], List(1, 2)))
		Assert.assertEquals(List(List(1), List(2)), res)
	}
}
