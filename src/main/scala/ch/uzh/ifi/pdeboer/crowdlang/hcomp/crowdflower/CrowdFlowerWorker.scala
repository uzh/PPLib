package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.crowdlang.hcomp.{FreetextAnswer, FreetextQuery, MultipleChoiceAnswer, MultipleChoiceQuery}
import dispatch._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created by Marc Tobler on 23.07.2014.
 * adapted by pdeboer on 10/10/14.
 *
 */
class CrowdFlowerWorker(val applicationName: String, apiKey: String, sandBox: Boolean) {
	val secureHost = host("api.crowdflower.com").secure

	def writeText(work: FreetextQuery) = {
		val request = new CrowdFlowerJobRequest(s"freetext by " + applicationName, work.question, apiKey)
		//TODO replaceall is very ugly here
		var cml = s"""<cml:textarea label="${work.question.replaceAll("\"", "")}" name="response" class="" instructions="" default="" validates="required"/>"""
		request.setCML(cml)
		val jobId = retry(2)(request.send(10 seconds))
		val job = new CFFreeTextJob(jobId, apiKey)
		job.addDataUnit("{}")
		job.launch(sandbox = this.sandBox)
		val timer = new GrowingTimer(start = 10 seconds, factor = 1.5, max = 1 minute)
		var answer: Option[String] = None
		while (answer.isEmpty) {
			timer.waitTime
			answer = job.fetchResult()
		}
		FreetextAnswer(work, answer.get)
	}

	def chooseOption(work: MultipleChoiceQuery) =
		if (work.maxNumberOfResults == work.minNumberOfResults && work.minNumberOfResults == 1) {
			chooseSingleOption(work)
		}
		else {
			chooseMultipleOptions(work)
		}


	/**
	 * Method used to retry some code that may fail n times.
	 * @param n  how often to retry
	 * @param fn  the fallible function
	 * @tparam T return value of the function
	 * @return the result of the function
	 */
	def retry[T](n: Int)(fn: => T): T = {
		try {
			fn
		} catch {
			case e if n > 1 =>
				retry(n - 1)(fn)
		}
	}

	private def chooseSingleOption(work: MultipleChoiceQuery) = {
		val request = new CrowdFlowerJobRequest(s"singlechoice by " + work.question, work.question, apiKey)
		var cml = s"""<cml:radios label="Choose one" class="" instructions="${work.question.replaceAll("\"", "")}" validates="required">"""
		work.options.foreach(option => cml += s"""<cml:radio label="${option}"/>""")
		cml += "</cml:radios>"
		request.setCML(cml)
		val jobId = retry(2)(request.send(10 seconds))
		var jsonString = "{"
		work.options.zipWithIndex.foreach(option => jsonString += s""""option_${option._2}" : "${option._1.replaceAll("\"", "")}", \n""")
		jsonString = jsonString.substring(0, jsonString.size - 3) //trim last comma
		jsonString += "}"
		val job = new CFSingleChoiceJob(jobId, apiKey)
		job.addDataUnit(jsonString)
		job.launch(sandbox = this.sandBox)
		val timer = new GrowingTimer(start = 30 seconds, factor = 2.0, max = 1 minute)
		var answer: Option[String] = None
		while (answer.isEmpty) {
			timer.waitTime
			answer = job.fetchResult()
		}
		val resultMap = collection.mutable.Map.empty[String, Boolean]
		work.options.foreach(option => resultMap += (option -> false))
		resultMap(answer.get) = true
		MultipleChoiceAnswer(work, resultMap.toMap[String, Boolean])
	}

	private def chooseMultipleOptions(work: MultipleChoiceQuery) = {
		//TODO Can only handle String data atm
		val stringOptions = List.empty[String] ++ work.options.map(_.toString)
		val request = new CrowdFlowerJobRequest(s"multiplechoice by " + applicationName, work.question, apiKey)
		var cml = s"""<cml:checkboxes label="Check all that apply" class="" instructions="${work.question.replaceAll("\"", "")}" validates="required">"""
		work.options.zipWithIndex.foreach(option => cml += s"""<cml:checkbox label="{{option_${option._2}}}"/>""")
		cml += "</cml:checkboxes>"
		request.setCML(cml)
		val jobId = retry(2)(request.send(10 seconds))
		var jsonString = "{"
		stringOptions.zipWithIndex.foreach(option => jsonString += s""""option_${option._2}" : "${option._1.replaceAll("\"", "")}", \n""")
		jsonString = jsonString.substring(0, jsonString.size - 3) //trim last comma
		jsonString += "}"
		val job = new CFMultipleChoiceJob(jobId, apiKey)
		job.addDataUnit(jsonString)
		job.launch(sandbox = this.sandBox)
		val timer = new GrowingTimer(start = 30 seconds, factor = 2.0, max = 1 minute)
		var answers: Option[List[String]] = None
		while (answers.isEmpty) {
			timer.waitTime
			answers = job.fetchResult()
		}
		val resultMap = collection.mutable.Map.empty[String, Boolean]
		work.options.foreach(option => resultMap += (option -> false))
		answers.get.foreach(answer => resultMap(answer) = true)
		MultipleChoiceAnswer(work, resultMap.toMap[String, Boolean])
	}

	/**
	 * Sends a HTTP request, awaits the response (current thread blocking) and returns the result als Json
	 * @param request The request to send
	 * @param timeout How long to wait for a response before the time out
	 * @return The response from the server parsed to json
	 * @throws InterruptedException     if the current thread is interrupted while waiting
	 * @throws TimeoutException         if after waiting for the specified time `awaitable` is still not ready
	 * @throws IllegalArgumentException if `timeout` is [[scala.concurrent.duration.Duration.Undefined D u r a t i o n.U n d e f i n e d]]
	 */
	private def sendAndAwaitJson(request: Req, timeout: Duration): JsValue = {
		val future = Http(request OK as.String).either
		var either: Either[Throwable, String] = Await.result(future, timeout)
		either match {
			case Right(content) =>
				val response: String = content
				val json: JsValue = Json.parse(response)
				json
			case Left(StatusCode(code)) =>
				throw StatusCode(code)
		}
	}

	/**
	 * Utility class that acts as a timer for the caller. Timer Duration grows on each call received.
	 * @param start the initial time duration (in ms)
	 * @param factor the factor by which the time increases on each call
	 * @param max the maximum duration of the timeout (in ms)
	 */
	class GrowingTimer(val start: Duration, val factor: Double, val max: Duration) {
		var currentTime = start

		/**
		 * Waits the current amount of time
		 */
		def waitTime = {
			Thread.sleep(currentTime.toMillis)
			updateTimer
		}

		private def updateTimer = {
			currentTime *= factor
			if (currentTime > max)
				currentTime = max
		}
	}

}
