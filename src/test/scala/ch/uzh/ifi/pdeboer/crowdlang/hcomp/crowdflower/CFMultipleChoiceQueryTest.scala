package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.crowdlang.hcomp.MultipleChoiceQuery
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 14/10/14.
 */
class CFMultipleChoiceQueryTest {
	@Test
	def testXMLMarshalling(): Unit = {
		val q = new CFMultipleChoiceQuery(new MultipleChoiceQuery("test?", List("test1", "test2"), 1, 1))
		Assert.assertEquals("<cml:checkboxes name=\"multipleChoice\" label=\"test?\" class=\"\" instructions=\"test?\" validates=\"required\"><cml:checkbox label=\"test1\"/><cml:checkbox label=\"test2\"/></cml:checkboxes>", q.getCML())
	}
}
