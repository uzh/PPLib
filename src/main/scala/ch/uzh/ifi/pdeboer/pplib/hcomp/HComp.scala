package ch.uzh.ifi.pdeboer.pplib.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter

import scala.collection.mutable

/**
 * Created by pdeboer on 10/10/14.
 */
object HComp {
	private val portals = new mutable.HashMap[Symbol, HCompPortalAdapter]()

	def addPortal(portal: HCompPortalAdapter) {
		addPortal(portal.getDefaultPortalKey, portal)
	}

	def addPortal(key: Symbol, portal: HCompPortalAdapter) {
		portals += (key -> portal)
	}

	def allDefinedPortals() = portals.values.filter(_ != null).toList

	def apply(key: Symbol) = portals(key)

	//convenience methods
	def crowdFlower: CrowdFlowerPortalAdapter = portals.get(CrowdFlowerPortalAdapter.PORTAL_KEY).get.asInstanceOf[CrowdFlowerPortalAdapter]

	def mechanicalTurk: CrowdFlowerPortalAdapter = portals.get('mechanicalTurk).get.asInstanceOf[CrowdFlowerPortalAdapter]

}
