package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import com.amazonaws.mturk.service.axis.RequesterService
import com.amazonaws.mturk.util.ClientConfig

/**
 * Created by pdeboer on 19/11/14.
 */
class MechanicalTurkPortalAdapter(accessKey: String, secretKey: String, sandbox: Boolean = false) extends HCompPortalAdapter {
	val serviceURL = if (sandbox) "https://mechanicalturk.sandbox.amazonaws.com/?Service=AWSMechanicalTurkRequester"
	else "https://mechanicalturk.amazonaws.com/?Service=AWSMechanicalTurkRequester"

	val service: RequesterService = new RequesterService(new ClientConfig() {
		setAccessKeyId(accessKey)
		setSecretAccessKey(secretKey)
		setServiceURL(serviceURL)
		setRetryAttempts(10)
		setRetryDelayMillis(1000)
	})

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		new MTurkManager(service).sendQueryAndWaitForResponse(query, properties)
	}

	override def getDefaultPortalKey: Symbol = ???

	override def cancelQuery(query: HCompQuery): Unit = ???
}
