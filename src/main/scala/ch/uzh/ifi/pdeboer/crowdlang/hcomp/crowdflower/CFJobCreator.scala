package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower


import ch.uzh.ifi.pdeboer.crowdlang.U
import ch.uzh.ifi.pdeboer.crowdlang.hcomp.HCompAnswer
import ch.uzh.ifi.pdeboer.crowdlang.util.GrowingTimer
import dispatch.Defaults._
import dispatch._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try

case class CFQP(name: String, cfName: String)

class CFQueryParameters(params: List[CFQP])

/**
 * Created by pdeboer on 15/10/14.
 * (with code from Marc Tobler's CrowdFlowerJob class)
 *
 * TODO at some point this needs to be refactored. Code is ugly
 */
class CFJobCreator(apiKey: String, query: CFQuery, sandbox: Boolean = true) {
	val jobResourceJSONUrl = apiURL / "jobs.json"
	private val apiURL = host("api.crowdflower.com").secure / "v1"
	var jobId: Int = -1
	var cachedResult: Option[HCompAnswer] = Option.empty[HCompAnswer]

	def performQuery() = {
		this.jobId = U.retry(2)(createJob(1 hour))
		launch()

		val timer = new GrowingTimer(start = 30 seconds, factor = 2.0, max = 1 minute)

		var answer: Option[HCompAnswer] = None
		while (answer.isEmpty) {
			timer.waitTime
			answer = fetchResult()
		}

		cachedResult = answer
		answer
	}

	/**
	 * creates job and returns job id
	 * @param timeout
	 * @return
	 */
	private def createJob(timeout: Duration): Int = {
		val future = Http(queryParameters OK as.String).either
		val either: Either[Throwable, String] = Await.result(future, timeout)
		either match {
			case Right(content) =>
				val response: String = content
				val json: JsValue = Json.parse(response)
				val jobId = (json \ "id").as[Int]
				jobId
			case Left(StatusCode(code)) =>
				throw StatusCode(code)
		}
	}

	private def queryParameters = {
		var req = jobResourceJSONUrl.POST.addQueryParameter("key", apiKey)
		req = req.addQueryParameter("job[title]", query.rawQuery.question)
		req = req.addQueryParameter("job[instructions]", query.rawQuery.question)
		req = req.addQueryParameter("job[judgments_per_unit]", "1")
		req = req.addQueryParameter("job[units_per_assignment]", "1")
		req = req.addQueryParameter("job[payment_cents]", "0")
		req = req.addQueryParameter("job[auto_order]", "true")
		req = req.addQueryParameter("job[cml]", query.getCML())
		req
	}

	private def fetchResult(): Option[HCompAnswer] = {
		println(s" $jobId : Fetching result.")
		val judgments_url = jobIdResourceURL / "judgments.json"
		var request = judgments_url.GET.addQueryParameter("key", apiKey)
		val json_try = Try(sendAndAwaitJson(request, 10 seconds))
		if (json_try.isFailure) {
			println(s" $jobId : Timed out")
			None
		}
		val json = json_try.get
		query.interpretResult(json)
	}

	def jobIdResourceURL = apiURL / "jobs" / jobId

	private def sendAndAwaitJson(request: Req, timeout: Duration) = {
		val future = Http(request OK as.String).either
		val either: Either[Throwable, String] = Await.result(future, timeout)
		either match {
			case Right(content) =>
				val response: String = content
				val json: JsValue = Json.parse(response)
				json
			case Left(StatusCode(code)) =>
				throw StatusCode(code)
		}
	}

	private def launch() {
		println(s"$jobId : launching.")
		val order_url = jobIdResourceURL / "orders.json"
		var request = order_url.POST.addQueryParameter("key", apiKey)
		request = request.addHeader("Content-Type", "application/x-www-form-urlencoded")
		if (sandbox)
			request = request.setBody(s"channels[0]=cf_internal&debit[units_count]=0")
		else
			request = request.setBody(s"channels[0]=on_demand&debit[units_count]=1")
		try {
			sendAndAwaitJson(request, 10 seconds)
		} catch {
			case e: TimeoutException =>
				println(s"Timed out: ${request.toRequest.toString}")
		}
	}

	private def addDataUnit(jsonString: String) = {
		val unitsURL = jobIdResourceURL / "upload.json"
		var units_request = unitsURL.POST.addQueryParameter("key", apiKey)
		units_request = units_request.addHeader("Content-Type", "application/json")
		units_request = units_request.setBody(jsonString)
		sendAndAwaitJson(units_request, 10 seconds)
	}
}
