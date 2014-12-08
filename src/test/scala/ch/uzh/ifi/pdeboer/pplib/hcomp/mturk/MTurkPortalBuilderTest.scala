package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 21/11/14.
 */
class MTurkPortalBuilderTest {
	@Test
	def testInitIncomplete: Unit = {
		val b = new MechanicalTurkPortalBuilder()
		b.setParameter(b.ACCESS_ID_KEY, "accessId")
		//omitting 2nd parameter should yield exception when creating instance

		try {
			b.build
			Assert.assertFalse("we have actually expected an exception here", true)
		}
		catch {
			case e: Throwable => Assert.assertTrue(true)
		}
	}

	@Test
	def testInitComplete: Unit = {
		val b = new MechanicalTurkPortalBuilder()
		b.setParameter(b.ACCESS_ID_KEY, "asdf")
		b.setParameter(b.SECRET_ACCESS_KEY, "fdsa")

		val p = b.build.asInstanceOf[MechanicalTurkPortalAdapter]
		Assert.assertEquals("asdf", p.accessKey)
		Assert.assertEquals("fdsa", p.secretKey)
	}
}
