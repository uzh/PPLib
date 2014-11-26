package ch.uzh.ifi.pdeboer.pplib.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.hcomp.mturk.MechanicalTurkPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.util.U
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

/**
 * Created by pdeboer on 10/10/14.
 */
object HComp extends LazyLogging {
	private val portals = new mutable.HashMap[String, HCompPortalAdapter]()

	def addPortal(portal: HCompPortalAdapter) {
		addPortal(portal.getDefaultPortalKey, portal)
	}

	def addPortal(key: String, portal: HCompPortalAdapter) {
		logger.info(s"adding portaladapter ${portal.getClass.getSimpleName} with key ${portal.getDefaultPortalKey}")
		portals += (key -> portal)
	}

	def allDefinedPortals = portals.values.filter(_ != null).toList

	def apply(key: String) = portals(key)

	//convenience methods, may be removed at a later stage
	def crowdFlower: CrowdFlowerPortalAdapter = portals.get(CrowdFlowerPortalAdapter.PORTAL_KEY).get.asInstanceOf[CrowdFlowerPortalAdapter]

	def mechanicalTurk: MechanicalTurkPortalAdapter = portals.get(MechanicalTurkPortalAdapter.PORTAL_KEY).get.asInstanceOf[MechanicalTurkPortalAdapter]

	protected def autoloadConfiguredPortals() {
		val config = ConfigFactory.load()

		val classes = U.findClassesInPackageWithProcessAnnotation("ch.uzh.ifi.pdeboer.pplib.hcomp", classOf[HCompPortal])
			.asInstanceOf[Set[Class[HCompPortalAdapter]]]
		val annotations = classes.map(_.getAnnotation(classOf[HCompPortal])).filter(_.autoInit)
		val builders = annotations.map(_.builder().newInstance())
		builders.foreach(b => {
			try {
				b.loadConfig(config)
				val portal: HCompPortalAdapter = b.build
				addPortal(portal.getDefaultPortalKey, portal)
			}
			catch {
				case e: Throwable => {
					val errorMessage: String = s"Skipped automatic initialization of $b due to missing / invalid configuration."
					logger.error(errorMessage)
					logger.debug(errorMessage, e)
				}
			}
		})

	}

	try {
		autoloadConfiguredPortals()
	}
	catch {
		case e: Throwable => logger.error("could not auto-initialize portals due to an error", e)
	}
}
