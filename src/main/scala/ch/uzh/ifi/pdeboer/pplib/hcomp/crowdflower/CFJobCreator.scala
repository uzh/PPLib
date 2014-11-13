package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower


import ch.uzh.ifi.pdeboer.pplib.U
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.util.GrowingTimer
import com.typesafe.scalalogging.LazyLogging
import dispatch.Defaults._
import dispatch._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try

abstract class CFJobBase(apiKey: String) extends LazyLogging {
	protected val apiURL = host("api.crowdflower.com").secure / "v1"
	protected val jobResourceJSONUrl = apiURL / "jobs.json"

	protected def sendAndAwaitJson(request: Req, timeout: Duration) = {
		val result = Http(request).map { response =>
			response.getStatusCode match {
				case code if code / 100 == 4 || code / 100 == 2 || code == 302 => (code, true, response.getResponseBody)
				case code => (code, false, new OkFunctionHandler(as.String).onCompleted(response))
			}
		}
		val data = Await.result(result, timeout)

		if (!data._2) {
			logger.info("couldnt finish job. problem: " + data._3) //output exact answer of server
			throw new StatusCode(data._1)
		}
		val response: String = data._3
		val json: JsValue = Json.parse(response)
		json
	}
}

class CFJobStatusManager(apiKey: String, jobId: Int) extends CFJobBase(apiKey) {
	def cancelQuery(maxTries: Int = 3, timeout: Duration = 30 seconds): Unit = {
		logger.info(s"$jobId : cancelling task")
		val cancelURL = jobIdResourceURL / "cancel"
		var request = cancelURL.PUT.addQueryParameter("key", apiKey)
		request = request.addHeader("Content-Type", "application/x-www-form-urlencoded")

		try {
			U.retry(maxTries) {
				sendAndAwaitJson(request, timeout)
			}
		} catch {
			case e: TimeoutException =>
				logger.error(s"Timed out: ${request.toRequest.toString}")
		}
	}

	def jobIdResourceURL = apiURL / "jobs" / jobId
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

	def performQuery(maxTries: Int = 1000000) = {
		addDataUnit("{}")
		launch()

		val timer = new GrowingTimer(start = 1 seconds, factor = 1.5, max = 1 minute)

		var answer: Option[HCompAnswer] = None
		U.retry(maxTries) {
			timer.waitTime
			answer = fetchResult()

			if (answer.isEmpty) throw new Exception() //continue waiting
		}

		cachedResult = answer
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
			var req: Req = jobResourceJSONUrl.POST.addQueryParameter("key", apiKey)
			req = req.addQueryParameter("job[cml]", query.getCML())

			val result = Http(parameters.fill(req)).map { response =>
				response.getStatusCode match {
					case code if code / 100 == 4 || code / 100 == 2 => (code, true, response.getResponseBody)
					case code => (code, false, new OkFunctionHandler(as.String).onCompleted(response))
				}
			}
			val data = Await.result(result, timeout)

			if (!data._2) {
				logger.info("couldnt create job. problem: " + data._3) //output exact answer of server
				throw new StatusCode(data._1)
			}
			val response: String = data._3
			val json: JsValue = Json.parse(response)
			jobId = (json \ "id").as[Int]
			jobId
		}
	}

	private def fetchResult(): Option[HCompAnswer] = {
		logger.info(s" $jobId : Fetching result for ${query.rawQuery.title}")
		val judgments_url = jobIdResourceURL / "judgments.json"
		var request = judgments_url.GET.addQueryParameter("key", apiKey)
		val json_try = Try(sendAndAwaitJson(request, 30 seconds))
		if (json_try.isFailure) {
			println(s" $jobId : Timed out")
			None
		}
		val json = json_try.get
		query.interpretResult(json)
	}

	private def launch() {
		logger.info(s"$jobId : launching task '${query.rawQuery.title}'")
		val order_url = jobIdResourceURL / "orders.json"
		var request = order_url.POST.addQueryParameter("key", apiKey)
		request = request.addHeader("Content-Type", "application/x-www-form-urlencoded")
		if (sandbox)
			request = request.setBody(s"channels[0]=cf_internal&debit[units_count]=1")
		else
			request = request.setBody(s"channels[0]=on_demand&debit[units_count]=1")

		try {
			U.retry(3) {
				sendAndAwaitJson(request, 30 seconds)
			}
		} catch {
			case e: Exception =>
				logger.error(s"Timed out: ${request.toRequest.toString}")
		}
	}

	def jobIdResourceURL = apiURL / "jobs" / jobId

	private def addDataUnit(jsonString: String) = {
		val unitsURL = jobIdResourceURL / "upload.json"
		var units_request = unitsURL.POST.addQueryParameter("key", apiKey)
		units_request = units_request.addHeader("Content-Type", "application/json")
		units_request = units_request.setBody(jsonString)
		U.retry(3) {
			sendAndAwaitJson(units_request, 30 seconds)
		}
	}
}

class CFQueryParameterSet(
							 title: String, instructions: String, judgementsPerUnit: Int = 1, unitsPerJudgement: Int = 1,
							 paymentCents: Double = 1d, autoOrder: Boolean = true) {

	def fill(request: Req) = {
		var ret = request.addQueryParameter("job[title]", title)
		ret = ret.addQueryParameter("job[instructions]", instructions)
		ret = ret.addQueryParameter("job[judgments_per_unit]", judgementsPerUnit + "")
		ret = ret.addQueryParameter("job[units_per_assignment]", unitsPerJudgement + "")
		ret = ret.addQueryParameter("job[payment_cents]", paymentCents + "")
		ret = ret.addQueryParameter("job[auto_order]", autoOrder + "")
		ret
	}
}
