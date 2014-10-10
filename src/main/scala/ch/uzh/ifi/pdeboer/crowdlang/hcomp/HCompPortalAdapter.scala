package ch.uzh.ifi.pdeboer.crowdlang.hcomp

import scala.concurrent.Future

/**
 * Created by pdeboer on 10/10/14.
 */
trait HCompPortalAdapter {
	def sendQuery(query: HCompQuery): Future[HCompAnswer]
}

trait HCompQuery {}

trait HCompAnswer {
	def query: HCompQuery
}

case class FreetextQuery(question: String) extends HCompQuery

case class FreetextAnswer(query: FreetextQuery, answer: String) extends HCompAnswer

//TODO add more types than just string
case class MultipleChoiceQuery(question: String, options: List[String], maxNumberOfResults: Int, minNumberOfResults: Int = 1) extends HCompQuery

case class MultipleChoiceAnswer(query: MultipleChoiceQuery, answer: Map[String, Boolean]) {
	def selectedAnswers: List[String] = answer.collect({
		case x if x._2 => x._1
	}).toList
}

case class HCompException(query: HCompQuery, exception: Throwable) extends HCompAnswer