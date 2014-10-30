package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.pplib.hcomp.MultipleChoiceQuery
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 14/10/14.
 */
class CFMultipleChoiceQueryTest {
	@Test
	def testXMLMarshalling(): Unit = {
		val q = new CFMultipleChoiceQuery(new MultipleChoiceQuery("test?", List("test1", "test2"), -1, 1))
		Assert.assertEquals( """<cml:checkboxes name="field" label="test?" class=" " instructions="test?" validates="required">
							   |		<cml:checkbox label="test1" value="0"/><cml:checkbox label="test2" value="1"/>
							   |	</cml:checkboxes>""".stripMargin, q.getCML())
	}
}
