package ch.uzh.ifi.pdeboer.pplib.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter
import com.typesafe.config.{Config, ConfigFactory}

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
		println(s"adding portaladapter ${portal.getClass.getSimpleName} with key ${portal.getDefaultPortalKey}")
		portals += (key -> portal)
	}

	def allDefinedPortals = portals.values.filter(_ != null).toList

	def apply(key: Symbol) = portals(key)

	//convenience methods
	def crowdFlower: CrowdFlowerPortalAdapter = portals.get(CrowdFlowerPortalAdapter.PORTAL_KEY).get.asInstanceOf[CrowdFlowerPortalAdapter]

	def mechanicalTurk: CrowdFlowerPortalAdapter = portals.get('mechanicalTurk).get.asInstanceOf[CrowdFlowerPortalAdapter]


	private def configContainsKey(key: String): Boolean =
		try {
			ConfigFactory.load().getString(key)
			true
		}
		catch {
			case _ => false
		}

	protected def autoloadConfiguredPortals() {
		val config: Config = ConfigFactory.load()

		if (configContainsKey(CrowdFlowerPortalAdapter.CONFIG_API_KEY))
			addPortal(CrowdFlowerPortalAdapter.PORTAL_KEY, new CrowdFlowerPortalAdapter("PPLib @ CrowdFlower"))

		//TODO add config directive to automatically load portals using reflection and their full classname
	}

	autoloadConfiguredPortals()
}
