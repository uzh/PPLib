package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * Created by pdeboer on 19/11/14.
 */
@HCompPortal(builder = classOf[MechanicalTurkPortalBuilder], autoInit = true)
class MechanicalTurkPortalAdapter(val accessKey: String, val secretKey: String, sandbox: Boolean = true, val approveAll: Boolean = true) extends HCompPortalAdapter with AnswerRejection with LazyLogger {
	val serviceURL = if (sandbox) "https://mechanicalturk.sandbox.amazonaws.com/?Service=AWSMechanicalTurkRequester"
	else "https://mechanicalturk.amazonaws.com/?Service=AWSMechanicalTurkRequester"

	var map = mutable.HashMap.empty[Int, MTurkQueries]

	val service = new MTurkService(accessKey, secretKey, new Server(serviceURL))

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		logger.info("registering query " + query.identifier)
		val manager: MTurkManager = new MTurkManager(service, query, properties, this)
		map += query.identifier -> map.getOrElse(query.identifier, new MTurkQueries()).add(manager)
		manager.createHIT()
		manager.waitForResponse()
	}

	override def getDefaultPortalKey: String = MechanicalTurkPortalAdapter.PORTAL_KEY

	override def cancelQuery(query: HCompQuery): Unit = {
		val managerOption = map.get(query.identifier)
		if (managerOption.isDefined) {
			managerOption.get.list.mpar.foreach(q => try {
				//naively cancel all previous queries just to make sure
				q._2.cancelHIT()
			}
			catch {
				case e: Exception => {}
			})
			logger.info(s"cancelled '${query.title}'")
		} else {
			logger.info(s"could not find query with ID '${query.identifier}' when trying to cancel it")
		}
	}

	protected[MechanicalTurkPortalAdapter] class MTurkQueries() {
		private var sent: List[(DateTime, MTurkManager)] = Nil

		def list = sent

		def add(manager: MTurkManager) = {
			this.synchronized {
				sent = (DateTime.now(), manager) :: sent
			}
			this
		}
	}

	def findAllUnapprovedHitsAndApprove: Unit = {
		var total: Int = 0
		var totalApproved: Int = 0
		import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
		service.SearchHITs().toList.mpar.foreach(h => {
			logger.info("processing hit " + h)
			total += 1
			try {
				service.GetAssignmentsForHIT(h.HITId).headOption match {
					case Some(x: Assignment) => try {
						if (!x.isApproved) {
							totalApproved += 1
							service.ApproveAssignment(x)
						}
					}
					catch {
						case e: Exception => logger.info("could not approve " + x)
					}
				}
			}
			catch {
				case e: Exception => logger.info("could not get assignments for hit " + h)
			}
		})
		logger.info(s"total: $total hits of which we approved $totalApproved")
	}

	def expireAllHits: Unit = {
		import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
		service.SearchHITs().toList.mpar.foreach(h => {
			try {
				if (h.Expiration.isAfterNow)
					service.DisableHIT(h.HITId)
			}
			catch {
				case e: Exception => logger.info(e.getMessage + "could not disable hit " + h)
			}
		})
	}

}

private[mturk] class RejectableTurkAnswer(a: Assignment, val answer: HCompAnswer, service: MTurkService) extends RejectableAnswer with LazyLogger {
	def reject(message: String) = try {
		service.RejectAssignment(a, message)
		true
	}
	catch {
		case e: Exception =>
			logger.error("couldn't reject assignment", e)
			false
	}

	def approve(message: String, bonusCents: Int = 0) = try {
		service.ApproveAssignment(a, message)
		true
	}
	catch {
		case e: Exception =>
			logger.error("couldn't approve assignment", e)
			false
	}
}

object MechanicalTurkPortalAdapter {
	val CONFIG_ACCESS_ID_KEY = "hcomp.mturk.accessKeyID"
	val CONFIG_SECRET_ACCESS_KEY = "hcomp.mturk.secretAccessKey"
	val CONFIG_SANDBOX_KEY = "hcomp.mturk.sandbox"
	val PORTAL_KEY = "mechanicalTurk"
}

class MechanicalTurkPortalBuilder extends HCompPortalBuilder {
	val ACCESS_ID_KEY: String = "accessKeyID"
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

case class MTurkWorker(id: String) extends HCompWorker {
	override def toString = id
}