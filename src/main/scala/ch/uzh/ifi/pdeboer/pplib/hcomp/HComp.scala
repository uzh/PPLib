package ch.uzh.ifi.pdeboer.pplib.hcomp

import ch.uzh.ifi.pdeboer.pplib.U
import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

/**
 * Created by pdeboer on 10/10/14.
 */
object HComp extends LazyLogging {
	private val portals = new mutable.HashMap[Symbol, HCompPortalAdapter]()

	def addPortal(portal: HCompPortalAdapter) {
		addPortal(portal.getDefaultPortalKey, portal)
	}

	def addPortal(key: Symbol, portal: HCompPortalAdapter) {
		logger.info(s"adding portaladapter ${portal.getClass.getSimpleName} with key ${portal.getDefaultPortalKey}")
		portals += (key -> portal)
	}

	def allDefinedPortals = portals.values.filter(_ != null).toList

	def apply(key: Symbol) = portals(key)

	//convenience methods
	def crowdFlower: CrowdFlowerPortalAdapter = portals.get(CrowdFlowerPortalAdapter.PORTAL_KEY).get.asInstanceOf[CrowdFlowerPortalAdapter]

	def mechanicalTurk: CrowdFlowerPortalAdapter = portals.get('mechanicalTurk).get.asInstanceOf[CrowdFlowerPortalAdapter]

	protected def autoloadConfiguredPortals() {
		if (U.getConfigString(CrowdFlowerPortalAdapter.CONFIG_API_KEY).isDefined)
			addPortal(CrowdFlowerPortalAdapter.PORTAL_KEY, new CrowdFlowerPortalAdapter("PPLib @ CrowdFlower"))
	}

	autoloadConfiguredPortals()
}
