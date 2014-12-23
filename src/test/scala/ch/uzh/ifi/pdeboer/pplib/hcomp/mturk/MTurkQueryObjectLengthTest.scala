package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp.FreetextQuery
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 23/12/14.
 */
class MTurkQueryObjectLengthTest {
	@Test
	def testLimitSimple: Unit = {
		val q = FreetextQuery("a" * 2000)
		val ftq = new MTFreeTextQuery(q)
		val res = ftq.limitLength()
		Assert.assertEquals("a" * 1888, res)
	}
}
