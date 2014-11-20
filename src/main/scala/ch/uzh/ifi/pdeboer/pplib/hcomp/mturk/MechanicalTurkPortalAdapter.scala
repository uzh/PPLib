package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.util.ClientConfig

import scala.collection.mutable

/**
 * Created by pdeboer on 19/11/14.
 */
class MechanicalTurkPortalAdapter(accessKey: String, secretKey: String, sandbox: Boolean = false) extends HCompPortalAdapter {
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
		val manager: MTurkManager = new MTurkManager(service, query, properties)
		map += query.identifier -> manager
		manager.createHIT()
		manager.waitForResponse()
	}

	override def getDefaultPortalKey: Symbol = 'MTurk

	override def cancelQuery(query: HCompQuery): Unit = {
		map(query.identifier).cancelHIT()
	}
}
