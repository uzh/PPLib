package ch.uzh.ifi.pdeboer.crowdlang.hcomp

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, _}


/**
 * Created by pdeboer on 10/10/14.
 */

trait HCompPortalAdapter {
	protected def processQuery(query: HCompQuery): Option[HCompAnswer]

	private var queryLog = List.empty[HCompQueryStats]

	def sendQuery(query: HCompQuery): Future[Option[HCompAnswer]] = Future {
		val timeBefore = System.currentTimeMillis()
		val answer = processQuery(query)
		val timeAfter = System.currentTimeMillis()

		//we risk the querylog to be incomplete if a query is being answered right now
		queryLog = HCompQueryStats(query, answer, timeAfter - timeBefore) :: queryLog

		answer
	}

	def sendQueryAndAwaitResult(query: HCompQuery, maxWaitTime: Duration = 2 days) = {
		val future = sendQuery(query)
		Await.result(future, maxWaitTime)
		future.value.get.get
	}

	def getDefaultPortalKey: String

	def getQueries() = queryLog
}

case class HCompQueryStats(query: HCompQuery, answer: Option[HCompAnswer], timeMillis: Long)

private object HCompIDGen {
	private val current = new AtomicInteger(0)

	def next() = current.incrementAndGet()
}

trait HCompQuery {
	def question: String

	final val identifier: Int = HCompIDGen.next()
}

trait HCompAnswer {
	def query: HCompQuery
}

trait HCompInstructions {
	def toString: String
}

object HCompConversions {
	implicit def hcompInstrToString(instr: HCompInstructions): String = instr.toString
}

case class CompositeQuery(queries: List[HCompQuery], question: String = "") extends HCompQuery

case class CompositeQueryAnswer(query: CompositeQuery, answers: Map[HCompQuery, Option[HCompAnswer]]) extends HCompAnswer {
	override def toString() = answers.map(q => q._1.question + "::" + q._2.getOrElse("[no answer]")).mkString("\n")
}

case class FreetextQuery(question: String, defaultAnswer: String = "") extends HCompQuery

case class FreetextAnswer(query: FreetextQuery, answer: String) extends HCompAnswer {
	override def toString() = answer
}

case class MultipleChoiceQuery(question: String, options: List[String], maxNumberOfResults: Int, minNumberOfResults: Int = 1) extends HCompQuery

case class MultipleChoiceAnswer(query: MultipleChoiceQuery, answer: Map[String, Boolean]) extends HCompAnswer {
	def selectedAnswers: List[String] = answer.collect({
		case x if x._2 => x._1
	}).toList

	def selectedAnswer = selectedAnswers(0)

	override def toString() = selectedAnswers.mkString(", ")
}

case class HCompException(query: HCompQuery, exception: Throwable) extends HCompAnswer