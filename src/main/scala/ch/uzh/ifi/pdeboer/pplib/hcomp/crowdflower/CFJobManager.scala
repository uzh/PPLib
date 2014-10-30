package ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower


import ch.uzh.ifi.pdeboer.pplib.U
import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompAnswer
import ch.uzh.ifi.pdeboer.pplib.util.GrowingTimer
import dispatch.Defaults._
import dispatch._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try

/**
 * Created by pdeboer on 15/10/14.
 * (with code from Marc Tobler's CrowdFlowerJob class)
 *
 * TODO at some point this needs to be refactored. Code is super ugly
 */
class CFJobManager(apiKey: String, query: CFQuery, sandbox: Boolean = true) {
	val jobResourceJSONUrl = apiURL / "jobs.json"
	private val apiURL = host("api.crowdflower.com").secure / "v1"
	var jobId: Int = -1
	var cachedResult: Option[HCompAnswer] = Option.empty[HCompAnswer]

	def performQuery(parameters: CFQueryParameterSet =
					 new CFQueryParameterSet(
						 query.rawQuery.title.take(100),
						 query.rawQuery.question), maxTries: Int = 100) = {
		this.jobId = U.retry(2)(createJob(1 hour, parameters))
		addDataUnit("{}")
		launch()

		val timer = new GrowingTimer(start = 1 seconds, factor = 1.5, max = 1 minute)

		var answer: Option[HCompAnswer] = None
		U.retry(maxTries)({
			timer.waitTime
			answer = fetchResult()

			if (answer.isEmpty) throw new Exception() //continue waiting
		})

		cachedResult = answer
		answer
	}

	/**
	 * creates job and returns job id
	 * @param timeout
	 * @return
	 */
	private def createJob(timeout: Duration, parameters: CFQueryParameterSet): Int = {
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
			println(data._3) //output exact answer of server
			throw new StatusCode(data._1)
		}
		val response: String = data._3
		val json: JsValue = Json.parse(response)
		val jobId = (json \ "id").as[Int]
		jobId
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

	private def launch() {
		println(s"$jobId : launching.")
		val order_url = jobIdResourceURL / "orders.json"
		var request = order_url.POST.addQueryParameter("key", apiKey)
		request = request.addHeader("Content-Type", "application/x-www-form-urlencoded")
		if (sandbox)
			request = request.setBody(s"channels[0]=cf_internal&debit[units_count]=1")
		else
			request = request.setBody(s"channels[0]=on_demand&debit[units_count]=1")
		try {
			U.retry(3)(sendAndAwaitJson(request, 10 seconds))
		} catch {
			case e: TimeoutException =>
				println(s"Timed out: ${request.toRequest.toString}")
		}
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

	private def addDataUnit(jsonString: String) = {
		val unitsURL = jobIdResourceURL / "upload.json"
		var units_request = unitsURL.POST.addQueryParameter("key", apiKey)
		units_request = units_request.addHeader("Content-Type", "application/json")
		units_request = units_request.setBody(jsonString)
		U.retry(3)(sendAndAwaitJson(units_request, 10 seconds))
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
