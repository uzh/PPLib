package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower


import dispatch.Defaults._
import dispatch._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent._
import scala.concurrent.duration.Duration

/**
 * Created by Marc Tobler on 30.07.2014.
 * adapted by pdeboer on 10/10/14.
 * @param title The title of the job
 * @param instructions The instructions for the job
 */
class CrowdFlowerJobRequest(title: String, instructions: String, apiKey: String) {
	// Default Constructor
	val secureHost = host("api.crowdflower.com").secure
	val jobsURL = secureHost / "v1" / "jobs.json"
	var underlying = jobsURL.POST.addQueryParameter("key", apiKey)
	underlying = underlying.addQueryParameter("job[title]", title)
	underlying = underlying.addQueryParameter("job[instructions]", instructions)
	underlying = underlying.addQueryParameter("job[judgments_per_unit]", "1")
	underlying = underlying.addQueryParameter("job[units_per_assignment]", "1")
	underlying = underlying.addQueryParameter("job[payment_cents]", "0")
	underlying = underlying.addQueryParameter("job[auto_order]", "true")

	//

	def setCountries(countries: List[String]) {

	}

	def setCML(cml: String) = {
		underlying = underlying.addQueryParameter("job[cml]", cml)
	}

	/**
	 * Sends the request, awaits the response (current thread blocking) and returns the result als Json
	 * @param timeout How long to wait for a response before the time out
	 * @return The jobId of the newly created task
	 * @throws InterruptedException     if the current thread is interrupted while waiting
	 * @throws TimeoutException         if after waiting for the specified time `awaitable` is still not ready
	 * @throws IllegalArgumentException if `timeout` is [[scala.concurrent.duration.Duration.Undefined D u r a t i o n.U n d e f i n e d]]
	 */
	def send(timeout: Duration): Int = {
		val future = Http(underlying OK as.String).either
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


}