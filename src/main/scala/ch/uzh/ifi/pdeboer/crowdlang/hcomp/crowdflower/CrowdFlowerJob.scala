package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower

import dispatch.Defaults._
import dispatch._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Try

/**
 * Created by Marc Tobler on 30.07.2014.
 * adapted by pdeboer on 10/10/14.
 */
abstract class CrowdFlowerJob[Output](val jobId: Int, val apiKey: String) {
	private val jobURL = host("api.crowdflower.com").secure / "v1" / "jobs" / jobId
	private var cachedResult: Option[Output] = None

	final def fetchResult(): Option[Output] = {
		if (cachedResult.isDefined)
			cachedResult
		else {
			println(s" $jobId : Fetching result.")
			val judgments_url = jobURL / "judgments.json"
			var request = judgments_url.GET.addQueryParameter("key", apiKey)
			val json_try = Try(sendAndAwaitJson(request, 10 seconds))
			if (json_try.isFailure) {
				println(s" $jobId : Timed out")
				return None
			}
			val json = json_try.get
			cachedResult = extractResult(json)
			cachedResult
		}
	}

	protected def extractResult(value: JsValue): Option[Output]

	def addDataUnit(jsonString: String) = {
		val unitsURL = jobURL / "upload.json"
		var units_request = unitsURL.POST.addQueryParameter("key", apiKey)
		units_request = units_request.addHeader("Content-Type", "application/json")
		units_request = units_request.setBody(jsonString)
		sendAndAwaitJson(units_request, 10 seconds)
	}

	def launch(sandbox: Boolean = false) {
		println(s"$jobId : launching.")
		val order_url = jobURL / "orders.json"
		var request = order_url.POST.addQueryParameter("key", apiKey)
		request = request.addHeader("Content-Type", "application/x-www-form-urlencoded")
		if (sandbox)
			request = request.setBody(s"channels[0]=cf_internal&debit[units_count]=1")
		else
			request = request.setBody(s"channels[0]=on_demand&debit[units_count]=1")
		try {
			sendAndAwaitJson(request, 10 seconds)
		} catch {
			case e: TimeoutException =>
				//TODO it works anyway, but how can i handle this?
				println(s"Timed out: ${request.toRequest.toString}")
		}
	}

	protected def sendAndAwaitJson(request: Req, timeout: Duration) = {
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

}
