package ch.uzh.ifi.pdeboer.pplib.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.hcomp.mturk.MechanicalTurkPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.util.U
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

	//convenience methods, may be removed at a later stage
	def crowdFlower: CrowdFlowerPortalAdapter = portals.get(CrowdFlowerPortalAdapter.PORTAL_KEY).get.asInstanceOf[CrowdFlowerPortalAdapter]

	def mechanicalTurk: MechanicalTurkPortalAdapter = portals.get(MechanicalTurkPortalAdapter.PORTAL_KEY).get.asInstanceOf[MechanicalTurkPortalAdapter]

	protected def autoloadConfiguredPortals() {
		//TODO introduce annotation to auto-init portals themselves
		if (U.getConfigString(CrowdFlowerPortalAdapter.CONFIG_API_KEY).isDefined)
			addPortal(CrowdFlowerPortalAdapter.PORTAL_KEY, new CrowdFlowerPortalAdapter("PPLib @ CrowdFlower"))

		if (U.getConfigString(MechanicalTurkPortalAdapter.CONFIG_ACCESS_ID_KEY).isDefined)
			addPortal(MechanicalTurkPortalAdapter.PORTAL_KEY, new MechanicalTurkPortalAdapter(
				U.getConfigString(MechanicalTurkPortalAdapter.CONFIG_ACCESS_ID_KEY).get,
				U.getConfigString(MechanicalTurkPortalAdapter.CONFIG_SECRET_ACCESS_KEY).get
			))
	}

	autoloadConfiguredPortals()
}
