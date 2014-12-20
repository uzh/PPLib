package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

import scala.collection.mutable

/**
 * Created by pdeboer on 19/11/14.
 */
@HCompPortal(builder = classOf[MechanicalTurkPortalBuilder], autoInit = true)
class MechanicalTurkPortalAdapter(val accessKey: String, val secretKey: String, sandbox: Boolean = true) extends HCompPortalAdapter with LazyLogger {
	val serviceURL = if (sandbox) "https://mechanicalturk.sandbox.amazonaws.com/?Service=AWSMechanicalTurkRequester"
	else "https://mechanicalturk.amazonaws.com/?Service=AWSMechanicalTurkRequester"

	var map = mutable.HashMap.empty[Int, MTurkManager]

	val service = new MTurkService(accessKey, secretKey, new Server(serviceURL))

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		val manager: MTurkManager = new MTurkManager(service, query, properties)
		map += query.identifier -> manager
		manager.createHIT()
		manager.waitForResponse()
	}

	override def getDefaultPortalKey: String = MechanicalTurkPortalAdapter.PORTAL_KEY

	override def cancelQuery(query: HCompQuery): Unit = {
		val managerOption: Option[MTurkManager] = map.get(query.identifier)
		if (managerOption.isDefined) {
			managerOption.get.cancelHIT()
			logger.info(s"cancelled '${query.title}'")
		} else {
			logger.info(s"could not find query '${query.question}' when trying to cancel it")
		}
	}
}

object MechanicalTurkPortalAdapter {
	val CONFIG_ACCESS_ID_KEY = "hcomp.mturk.accessKeyID"
	val CONFIG_SECRET_ACCESS_KEY = "hcomp.mturk.secretAccessKey"
	val CONFIG_SANDBOX_KEY = "hcomp.mturk.sandbox"
	val PORTAL_KEY = "mechanicalTurk"
}

class MechanicalTurkPortalBuilder extends HCompPortalBuilder {
	val ACCESS_ID_KEY: String = "accessIdKey"
	val SECRET_ACCESS_KEY: String = "secretAccessKey"
	val SANDBOX: String = "sandbox"

	val parameterToConfigPath = Map(
		ACCESS_ID_KEY -> MechanicalTurkPortalAdapter.CONFIG_ACCESS_ID_KEY,
		SECRET_ACCESS_KEY -> MechanicalTurkPortalAdapter.CONFIG_SECRET_ACCESS_KEY,
		SANDBOX -> MechanicalTurkPortalAdapter.CONFIG_SANDBOX_KEY
	)

	override def build: HCompPortalAdapter = new MechanicalTurkPortalAdapter(
		params(ACCESS_ID_KEY),
		params(SECRET_ACCESS_KEY),
		params.getOrElse(SANDBOX, "false") == "true"
	)

	override def expectedParameters: List[String] = List(ACCESS_ID_KEY, SECRET_ACCESS_KEY)
}