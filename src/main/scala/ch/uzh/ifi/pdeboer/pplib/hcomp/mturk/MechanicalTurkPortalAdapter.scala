package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import ch.uzh.ifi.pdeboer.pplib.util.{GrowingTimer, U, LazyLogger}
import org.joda.time.DateTime

import scala.collection.mutable

/**
  * Created by pdeboer on 19/11/14.
  */
@HCompPortal(builder = classOf[MechanicalTurkPortalBuilder], autoInit = true)
class MechanicalTurkPortalAdapter(val accessKey: String, val secretKey: String, val sandbox: Boolean = true, var approveAll: Boolean = true, val waitingTimeGrowthFactorPerQuery: Double = 1.5d) extends HCompPortalAdapter with AnswerRejection with ForcedQueryPolling with LazyLogger {
	val serviceURL = if (sandbox) "https://mechanicalturk.sandbox.amazonaws.com/?Service=AWSMechanicalTurkRequester"
	else "https://mechanicalturk.amazonaws.com/?Service=AWSMechanicalTurkRequester"

	var mtQueriesForID = mutable.HashMap.empty[Int, MTurkQueries]

	val service = new MTurkService(accessKey, secretKey, new Server(serviceURL))

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		logger.info("registering query " + query.identifier)
		try {
			val manager: MTurkManager = new MTurkManager(query, properties, this, appropriateTimerIntervalSeconds)
			val registeredMTQueriesForThisQuery: MTurkQueries = mtQueriesForID.getOrElse(query.identifier, new MTurkQueries())
			mtQueriesForID += query.identifier -> registeredMTQueriesForThisQuery.add(manager)
			manager.createHIT()
			val answer = manager.waitForResponse()
			registeredMTQueriesForThisQuery.setFinished(answer)
			answer
		} catch {
			case e: Throwable => logger.error("unexpected exception at process query. Returning no answer", e); None
		}
	}

	override def getDefaultPortalKey: String = MechanicalTurkPortalAdapter.PORTAL_KEY

	override def cancelQuery(query: HCompQuery): Unit = {
		val managerOption = mtQueriesForID.get(query.identifier)
		if (managerOption.isDefined) {
			managerOption.get.list.mpar.foreach(q => try {
				//naively cancel all previous queries just to make sure
				q._2.cancelHIT()
			}
			catch {
				case e: Exception => {}
			})
			managerOption.get.setFinished(None)
			logger.info(s"cancelled '${query.title}'")
		} else {
			logger.info(s"could not find query with ID '${query.identifier}' when trying to cancel it")
		}
	}

	def appropriateTimerIntervalSeconds = {
		val numberOfRunningQueries = mtQueriesForID.values.filterNot(_.isFinished).size
		(numberOfRunningQueries.toDouble * waitingTimeGrowthFactorPerQuery).toInt
	}

	protected[MechanicalTurkPortalAdapter] class MTurkQueries() {
		private var sent: List[(DateTime, MTurkManager)] = Nil

		private var answer: Option[HCompAnswer] = None
		private var _isFinished: Boolean = false

		def setFinished(answer: Option[HCompAnswer]): Unit = {
			this.answer = answer
			_isFinished = true
		}

		def isFinished: Boolean = _isFinished


		def list = sent

		def add(manager: MTurkManager) = {
			serviceURL.synchronized {
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

	override def poll(query: HCompQuery): Unit = mtQueriesForID(query.identifier).list.foreach(q => q._2.forcePoll())
}

private[mturk] class RejectableTurkAnswer(a: Assignment, val answer: HCompAnswer, service: MTurkService) extends RejectableAnswer with LazyLogger {
	private var untouched: Boolean = true

	import scala.concurrent.duration._

	def reject(message: String) = if (untouched) try {
		U.retry(3, new GrowingTimer(1 second, 30, 300 seconds))(service.RejectAssignment(a, message))
		untouched = false
		true
	}
	catch {
		case e: Exception =>
			logger.error("couldn't reject assignment", e)
			false
	} else false

	def approve(message: String, bonusCents: Int = 0) = if (untouched) try {
		U.retry(3, new GrowingTimer(1 second, 30, 300 seconds))(service.ApproveAssignment(a, message))
		untouched = false
		true
	}
	catch {
		case e: Exception =>
			logger.error("couldn't approve assignment", e)
			false
	} else false
}

object MechanicalTurkPortalAdapter {
	val CONFIG_ACCESS_ID_KEY = "accessKeyID"
	val CONFIG_SECRET_ACCESS_KEY = "secretAccessKey"
	val CONFIG_SANDBOX_KEY = "sandbox"
	val CONFIG_WAITING_TIME_GROWTH_FACTOR_PER_QUERY = "waitingTimeGrowthFactorPerQuery"
	val PORTAL_KEY = "mechanicalTurk"
}

class MechanicalTurkPortalBuilder extends HCompPortalBuilder {
	val ACCESS_ID_KEY: String = "accessKeyID"
	val SECRET_ACCESS_KEY: String = "secretAccessKey"
	val SANDBOX: String = "sandbox"
	val WAITING_TIME_GROWTH_FACTOR_PER_QUERY: String = "waitingTimeGrowthFactorPerQuery"

	val parameterToConfigPath = Map(
		ACCESS_ID_KEY -> MechanicalTurkPortalAdapter.CONFIG_ACCESS_ID_KEY,
		SECRET_ACCESS_KEY -> MechanicalTurkPortalAdapter.CONFIG_SECRET_ACCESS_KEY,
		SANDBOX -> MechanicalTurkPortalAdapter.CONFIG_SANDBOX_KEY,
		WAITING_TIME_GROWTH_FACTOR_PER_QUERY -> MechanicalTurkPortalAdapter.CONFIG_WAITING_TIME_GROWTH_FACTOR_PER_QUERY
	)

	override def key = MechanicalTurkPortalAdapter.PORTAL_KEY

	override def build: HCompPortalAdapter = new MechanicalTurkPortalAdapter(
		params(ACCESS_ID_KEY),
		params(SECRET_ACCESS_KEY),
		params.getOrElse(SANDBOX, "false") == "true",
		waitingTimeGrowthFactorPerQuery = params.getOrElse(WAITING_TIME_GROWTH_FACTOR_PER_QUERY, "1.5").toDouble
	)

	override def expectedParameters: List[String] = List(ACCESS_ID_KEY, SECRET_ACCESS_KEY)
}

case class MTurkWorker(id: String) extends HCompWorker {
	override def toString = id
}