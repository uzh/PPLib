package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import com.typesafe.config.ConfigFactory

/**
 * Created by pdeboer on 10/10/14.
 */
class CrowdFlowerPortalAdapter(applicationName: String, apiKey: String, sandbox: Boolean = false) extends HCompPortalAdapter {

	def this(applicationName: String, sandbox: Boolean) =
		this(applicationName,
			ConfigFactory.load().getString(CrowdFlowerPortalAdapter.CONFIG_API_KEY),
			sandbox)

	def this(applicationName: String) = this(applicationName, false)

	override def getDefaultPortalKey: Symbol = CrowdFlowerPortalAdapter.PORTAL_KEY

	protected override def processQuery(query: HCompQuery) = {
		val cfQuery: CFQuery = CFConversions.convertQueryToCFQuery(query)
		val jobManager = new CFJobManager(apiKey, cfQuery, sandbox)
		jobManager.performQuery()
	}
}

object CrowdFlowerPortalAdapter {
	val CONFIG_API_KEY = "hcomp.crowdflower.apikey"
	val PORTAL_KEY = 'crowdFlower
}