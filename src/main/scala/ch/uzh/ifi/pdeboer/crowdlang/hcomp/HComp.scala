package ch.uzh.ifi.pdeboer.crowdlang.hcomp

import ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower.CrowdFlowerPortalAdapter

import scala.collection.mutable

/**
 * Created by pdeboer on 10/10/14.
 */
object HComp {
	private val portals = new mutable.HashMap[String, HCompPortalAdapter]()

	def addPortal(portal: HCompPortalAdapter) {
		addPortal(portal.getDefaultPortalKey, portal)
	}

	def addPortal(key: String, portal: HCompPortalAdapter) {
		portals += (key -> portal)
	}

	//convenience methods
	def crowdFlower: CrowdFlowerPortalAdapter = portals.get("crowdFlower").get.asInstanceOf[CrowdFlowerPortalAdapter]

	def mechanicalTurk: CrowdFlowerPortalAdapter = portals.get("mechanicalTurk").get.asInstanceOf[CrowdFlowerPortalAdapter]

	def allDefinedPortals() = portals.values.filter(_ != null).toList
}
