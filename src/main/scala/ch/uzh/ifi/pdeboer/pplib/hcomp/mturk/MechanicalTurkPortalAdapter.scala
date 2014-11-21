package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.util.ClientConfig
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

/**
 * Created by pdeboer on 19/11/14.
 */
class MechanicalTurkPortalAdapter(accessKey: String, secretKey: String, sandbox: Boolean = true) extends HCompPortalAdapter with LazyLogging {
	val serviceURL = if (sandbox) "https://mechanicalturk.sandbox.amazonaws.com/?Service=AWSMechanicalTurkRequester"
	else "https://mechanicalturk.amazonaws.com/?Service=AWSMechanicalTurkRequester"

	var map = mutable.HashMap.empty[Int, MTurkManager]

	val service: RequesterService = new RequesterService(new ClientConfig() {
		setAccessKeyId(accessKey)
		setSecretAccessKey(secretKey)
		setServiceURL(serviceURL)
		setRetryAttempts(10)
		setRetryDelayMillis(1000)
	})

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		logger.info("executing query " + query)
		val manager: MTurkManager = new MTurkManager(service, query, properties)
		map += query.identifier -> manager
		manager.createHIT()
		manager.waitForResponse()
	}

	override def getDefaultPortalKey: String = MechanicalTurkPortalAdapter.PORTAL_KEY

	override def cancelQuery(query: HCompQuery): Unit = {
		map(query.identifier).cancelHIT()
	}
}

object MechanicalTurkPortalAdapter {
	val CONFIG_ACCESS_ID_KEY = "hcomp.mturk.accessKeyID"
	val CONFIG_SECRET_ACCESS_KEY = "hcomp.mturk.secretAccessKey"
	val PORTAL_KEY = "mechanicalTurk"
}