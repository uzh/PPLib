package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.pplib.hcomp.FreetextQuery
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 14/10/14.
 */
class CFFreetextQueryTest {
	@Test
	def testXMLMarshallingDefaultAnswer(): Unit = {
		val q = new CFFreetextQuery(FreetextQuery("mylabel", "default"), "mynam\"e")
		Assert.assertEquals(
			"<cml:textarea name=\"mynam&quot;e\" label=\"mylabel\" default=\"default\" class=\" \" instructions=\" \" validates=\"\"/>",
			q.getCML())
	}

	@Test
	def testXMLMarshallingNoDefaultAnswer(): Unit = {
		val q = new CFFreetextQuery(FreetextQuery("mylabel"), "mynam\"e")
		Assert.assertEquals(
			"<cml:textarea name=\"mynam&quot;e\" label=\"mylabel\" default=\"\" class=\" \" instructions=\" \" validates=\"required\"/>",
			q.getCML())
	}
}
