package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Created by pdeboer on 10/10/14.
 */
@HCompPortal(builder = classOf[CrowdFlowerPortalBuilder], autoInit = true)
class CrowdFlowerPortalAdapter(val applicationName: String, val apiKey: String, sandbox: Boolean = false) extends HCompPortalAdapter {
	private var jobIds = collection.mutable.HashMap.empty[Int, CFJobCreator]

	def this(applicationName: String, sandbox: Boolean) =
		this(applicationName,
			ConfigFactory.load().getString(CrowdFlowerPortalAdapter.CONFIG_API_KEY),
			sandbox)

	def this(applicationName: String) = this(applicationName, false)

	override def getDefaultPortalKey: String = CrowdFlowerPortalAdapter.PORTAL_KEY

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
	val CONFIG_APPLICATION_NAME = "hcomp.crowdflower.applicationName"
	val CONFIG_SANDBOX = "hcomp.crowdflower.sandbox"

	val PORTAL_KEY = "crowdFlower"
}

class CrowdFlowerPortalBuilder extends HCompPortalBuilder {
	val API_KEY: String = "apiKey"
	val APPLICATION_NAME: String = "appName"
	val SANDBOX: String = "sandbox"

	val parameterToConfigPath = Map(
		API_KEY -> CrowdFlowerPortalAdapter.CONFIG_API_KEY,
		APPLICATION_NAME -> CrowdFlowerPortalAdapter.CONFIG_APPLICATION_NAME,
		SANDBOX -> CrowdFlowerPortalAdapter.CONFIG_SANDBOX
	)

	override def build: HCompPortalAdapter = new CrowdFlowerPortalAdapter(
		params.getOrElse(APPLICATION_NAME, "PPLib Application"),
		params(API_KEY), params.getOrElse(SANDBOX, "false") == "true"
	)

	override def expectedParameters: List[String] = List(API_KEY)
}