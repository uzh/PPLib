package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.pplib.hcomp.MultipleChoiceQuery
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 14/10/14.
 */
class CFSingleChoiceQueryTest {
	@Test
	def testXMLMarshalling(): Unit = {
		val q = new CFSingleChoiceQuery(new MultipleChoiceQuery("test?", List("test1", "test2"), 1, 1))
		Assert.assertEquals( """<cml:radios name="field" label="test?" class=" " instructions="test?" validates="required">
							   |		<cml:radio label="test1" value="0"/><cml:radio label="test2" value="1"/>
							   |	</cml:radios>""".stripMargin, q.getCML())
	}
}
