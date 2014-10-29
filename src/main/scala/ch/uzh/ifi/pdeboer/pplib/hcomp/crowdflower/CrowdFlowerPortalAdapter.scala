package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.pplib.hcomp._

/**
 * Created by pdeboer on 10/10/14.
 */
class CrowdFlowerPortalAdapter(applicationName: String, apiKey: String, sandbox: Boolean = false) extends HCompPortalAdapter {
	override def getDefaultPortalKey: String = "crowdFlower"

	protected override def processQuery(query: HCompQuery) = {
		val cfQuery: CFQuery = CFConversions.convertQueryToCFQuery(query)
		val jobManager = new CFJobManager(apiKey, cfQuery, sandbox)
		jobManager.performQuery()
	}
}
