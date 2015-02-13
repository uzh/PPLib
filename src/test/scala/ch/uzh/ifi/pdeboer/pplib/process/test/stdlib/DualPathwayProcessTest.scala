package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal.RandomHCompPortal
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.DualPathwayProcess
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 11/11/14.
 */
class DualPathwayProcessTest {
	@Test
	def testDynamicInstanciability: Unit = {
		val constructor = classOf[DualPathwayProcess].getConstructor(classOf[Map[String, Any]])
		constructor.newInstance(Map.empty[String, Any])
		Assert.assertTrue(true) //exception if it fails
	}

	@Test
	def testPortalParam = {
		val portal = new RandomHCompPortal("bla")
		val d = new DualPathwayProcess(Map(DefaultParameters.PORTAL_PARAMETER.key -> portal))
		Assert.assertEquals(d.portal.decoratedPortal, portal)
	}
}
