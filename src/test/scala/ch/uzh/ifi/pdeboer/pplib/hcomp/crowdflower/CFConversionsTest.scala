package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.pplib.hcomp.MultipleChoiceQuery
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 30/10/14.
 */
class CFConversionsTest {
	@Test
	def testConversionMultipleChoice: Unit = {
		val multipleChoice = CFConversions.convertQueryToCFQuery(MultipleChoiceQuery("test", List("a", "b"), 2, 1))
		Assert.assertTrue(multipleChoice.isInstanceOf[CFMultipleChoiceQuery])
	}

	@Test
	def testConversionSingleChoice: Unit = {
		val singleChoice = CFConversions.convertQueryToCFQuery(MultipleChoiceQuery("test", List("a", "b"), 1, 1))
		Assert.assertTrue(singleChoice.isInstanceOf[CFSingleChoiceQuery])
	}
}
