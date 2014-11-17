package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompJobCancelled, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.util._
import com.typesafe.scalalogging.LazyLogging
import dispatch._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try

class CFURLBuilder(restMethod: String) extends URLBuilder("https", "api.crowdflower.com", 443, "/v1/" + restMethod)

abstract class CFJobBase(apiKey: String) extends LazyLogging {
	protected val jobResourceJSONUrl = new CFURLBuilder("jobs.json")

	protected def sendAndAwaitJson(request: RESTClient, timeout: Duration) = {
		val body = Await.result(request.responseBody, timeout)
		val statusCode: Int = body.statusCode
		if (!body.isOk) {
			val error = new IllegalStateException(s"got NON-OK status return code: $statusCode with body ${body.body}")
			logger.error("err", error)
			throw error
		}

		val json: JsValue = Json.parse(body.body)
		logger.debug(s"got data $json")
		json
	}
}

class CFJobStatusManager(apiKey: String, jobId: Int) extends CFJobBase(apiKey) {
	def cancelQuery(maxTries: Int = 3): Unit = {
		logger.info(s"$jobId : cancelling task")
		val cancelURL = jobIdResourceURL / "cancel"
		val request = new PUT(cancelURL.addQueryParameter("key", apiKey) + "")
		request.headers += "Content-Type" -> "application/x-www-form-urlencoded"

		U.retry(maxTries) {
			sendAndAwaitJson(request, 30 seconds)
		}
	}

	def jobIdResourceURL = new CFURLBuilder("jobs") / s"$jobId"
}

/**
 * Created by pdeboer on 15/10/14.
 * (with code from Marc Tobler's CrowdFlowerJob class)
 *
 * TODO at some point this needs to be refactored. Code is super ugly
 */
class CFJobCreator(apiKey: String, query: CFQuery, properties: HCompQueryProperties, sandbox: Boolean = false) extends CFJobBase(apiKey) {
	var jobId: Int = -1
	var cachedResult: Option[HCompAnswer] = Option.empty[HCompAnswer]
	private var scheduledForCancellation: Boolean = false

	def cancel(): Unit = {
		scheduledForCancellation = true
	}

	def performQuery(maxTries: Int = 1000000) = {
		addDataUnit("{}")
		launch()

		val timer = new GrowingTimer(start = 1 seconds, factor = 1.5, max = 1 minute)

		var answer: Option[HCompAnswer] = None
		U.retry(maxTries) {
			timer.waitTime
			//throw exceptions until maxtries exceeded. wow, this is super ugly
			if (scheduledForCancellation) throw new Exception()

			answer = fetchResult()

			if (answer.isEmpty) throw new Exception() //continue waiting
		}

		cachedResult = if (scheduledForCancellation) Some(HCompJobCancelled(query.rawQuery)) else answer
		answer
	}

	/**
	 * creates job and returns job id
	 * @param timeout
	 * @return
	 */
	def createJob(parameters: CFQueryParameterSet =
				  new CFQueryParameterSet(
					  query.rawQuery.title.take(100),
					  query.rawQuery.question,
					  paymentCents = properties.paymentCents), timeout: Duration = 10 minutes): Int = {

		U.retry(3) {
			val req = new POST(jobResourceJSONUrl.addQueryParameter("key", apiKey).toString)
			parameters.apply(req)
			req.parameters += ("job[cml]" -> query.getCML())

			val data = Await.result(req.responseBody, timeout)

			if (!data.isOk) {
				val exc = new IllegalStateException(s"couldnt create job. statuscode was ${data.statusCode} problem: ${data.body}") //output exact answer of server
				logger.error("", exc)
				throw exc
			}
			try {
				val json: JsValue = Json.parse(data.body)
				jobId = (json \ "id").as[Int]
				jobId
			}
			catch {
				case e: Throwable => {
					logger.error("could not start job. May try again. Full JSON: " + data.body, e)
					throw new IllegalStateException("could not start job", e)
				}
			}
		}
	}

	private def fetchResult(): Option[HCompAnswer] = {
		logger.info(s" $jobId : Fetching result for ${query.rawQuery.title}")
		val judgmentsURL = jobIdResourceURL / "judgments.json"
		val request = new GET(judgmentsURL.addQueryParameter("key", apiKey).toString)
		val json_try = Try(sendAndAwaitJson(request, 30 seconds))
		if (json_try.isFailure) {
			logger.error(s" $jobId : Timed out")
			None
		}
		val json = json_try.get
		query.interpretResult(json)
	}

	private def launch() {
		logger.info(s"$jobId : launching task '${query.rawQuery.title}'")
		val orderURL = jobIdResourceURL / "orders.json"
		val request = new POST(orderURL.addQueryParameter("key", apiKey) + "")
		val params = if (sandbox)
			Map("channels[0]" -> "cf_internal", "debit[units_count]" -> "1")
		else
			Map("channels[0]" -> "on_demand", "debit[units_count]" -> "1")

		params.foreach {
			case (key, value) => request.parameters += key -> value
		}

		U.retry(3) {
			sendAndAwaitJson(request, 30 seconds)
		}
	}

	def jobIdResourceURL = new CFURLBuilder("jobs") / s"$jobId"

	private def addDataUnit(jsonString: String) = {
		val unitsURL = jobIdResourceURL / "upload.json"
		val request = new POST(unitsURL.addQueryParameter("key", apiKey) + "")
		request.headers += "Content-Type" -> "application/json"
		request.bodyString = jsonString
		U.retry(3) {
			sendAndAwaitJson(request, 30 seconds)
		}
	}
}

class CFQueryParameterSet(
							 title: String, instructions: String, judgementsPerUnit: Int = 1, unitsPerJudgement: Int = 1,
							 paymentCents: Double = 1d, autoOrder: Boolean = true) {

	def apply(request: RESTMethodWithBody) {
		request.parameters += "job[title]" -> title
		request.parameters += "job[instructions]" -> instructions
		request.parameters += "job[judgments_per_unit]" -> judgementsPerUnit.toString
		request.parameters += "job[units_per_assignment]" -> unitsPerJudgement.toString
		request.parameters += "job[payment_cents]" -> paymentCents.toString
		request.parameters += "job[auto_order]" -> autoOrder.toString
	}
}
