package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.crowdlang.hcomp.FreetextQuery
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 14/10/14.
 */
class CFFreetextQueryTest {
	@Test
	def testXMLMarshalling(): Unit = {
		val q = new CFFreetextQuery(FreetextQuery("mylabel"), "mynam\"e")
		Assert.assertEquals("<cml:textarea name=\"mynam&quot;e\" label=\"mylabel\" class=\" \" instructions=\" \" default=\" \" validates=\"required\"/>", q.getCML())
	}
}
