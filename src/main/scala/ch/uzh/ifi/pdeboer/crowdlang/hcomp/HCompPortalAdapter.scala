package ch.uzh.ifi.pdeboer.crowdlang.hcomp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, _}


/**
 * Created by pdeboer on 10/10/14.
 */

trait HCompPortalAdapter {
	protected def processQuery(query: HCompQuery): HCompAnswer

	private var queryLog = List.empty[HCompQueryStats]

	def sendQuery(query: HCompQuery): Future[HCompAnswer] = Future {
		val timeBefore = System.currentTimeMillis()
		val answer: HCompAnswer = processQuery(query)
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

	def getQueries() = queryLog
}

case class HCompQueryStats(query: HCompQuery, answer: HCompAnswer, timeMillis: Long)

trait HCompQuery {}

trait HCompAnswer {
	def query: HCompQuery
}

case class FreetextQuery(question: String) extends HCompQuery

case class FreetextAnswer(query: FreetextQuery, answer: String) extends HCompAnswer

//TODO add more types than just string
case class MultipleChoiceQuery(question: String, options: List[String], maxNumberOfResults: Int, minNumberOfResults: Int = 1) extends HCompQuery

case class MultipleChoiceAnswer(query: MultipleChoiceQuery, answer: Map[String, Boolean]) extends HCompAnswer {
	def selectedAnswers: List[String] = answer.collect({
		case x if x._2 => x._1
	}).toList
}

case class HCompException(query: HCompQuery, exception: Throwable) extends HCompAnswer