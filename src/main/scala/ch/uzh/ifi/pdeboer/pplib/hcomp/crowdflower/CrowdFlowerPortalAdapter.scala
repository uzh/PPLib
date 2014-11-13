package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import com.typesafe.config.ConfigFactory

/**
 * Created by pdeboer on 10/10/14.
 */
class CrowdFlowerPortalAdapter(applicationName: String, apiKey: String, sandbox: Boolean = false) extends HCompPortalAdapter {
	private var jobIds = collection.mutable.HashMap.empty[Int, CFJobCreator]

	def this(applicationName: String, sandbox: Boolean) =
		this(applicationName,
			ConfigFactory.load().getString(CrowdFlowerPortalAdapter.CONFIG_API_KEY),
			sandbox)

	def this(applicationName: String) = this(applicationName, false)

	override def getDefaultPortalKey: Symbol = CrowdFlowerPortalAdapter.PORTAL_KEY

	//TODO make this protected
	override def processQuery(query: HCompQuery, properties: HCompQueryProperties) = {
		val cfQuery: CFQuery = CFConversions.convertQueryToCFQuery(query)
		val jobCreator = new CFJobCreator(apiKey, cfQuery, properties, sandbox)
		jobIds += query.identifier -> jobCreator
		jobCreator.createJob()
		val res = jobCreator.performQuery()
		if (sandbox) cancelQuery(query) // in CF one needs to cancel the query afterwards
		res
	}


	override def cancelQuery(query: HCompQuery): Unit = {
		val manager: CFJobCreator = jobIds(query.identifier)
		new CFJobStatusManager(apiKey, manager.jobId).cancelQuery()

	}
}

object CrowdFlowerPortalAdapter {
	val CONFIG_API_KEY = "hcomp.crowdflower.apikey"
	val PORTAL_KEY = 'crowdFlower
}