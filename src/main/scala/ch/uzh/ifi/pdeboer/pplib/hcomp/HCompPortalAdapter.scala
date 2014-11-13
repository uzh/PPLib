package ch.uzh.ifi.pdeboer.pplib.hcomp

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, _}


/**
 * Created by pdeboer on 10/10/14.
 */

trait HCompPortalAdapter {

	//TODO we should hide this method somehow to the public
	def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer]

	private var queryLog = List.empty[HCompQueryStats]

	def sendQuery(query: HCompQuery, properties: HCompQueryProperties = HCompQueryProperties()): Future[Option[HCompAnswer]] = Future {
		val timeBefore = System.currentTimeMillis()
		val answer = processQuery(query, properties)
		val timeAfter = System.currentTimeMillis()

		//we risk the querylog to be incomplete if a query is being answered right now
		queryLog = HCompQueryStats(query, answer, timeAfter - timeBefore, properties.paymentCents) :: queryLog

		answer
	}

	def sendQueryAndAwaitResult(query: HCompQuery, properties: HCompQueryProperties = HCompQueryProperties(), maxWaitTime: Duration = 2 days) = {
		val future = sendQuery(query)
		Await.result(future, maxWaitTime)
		future.value.get.get
	}

	def getDefaultPortalKey: Symbol

	def getQueries() = queryLog
}

class CostCountingEnabledHCompPortal(decoratedPortal: HCompPortalAdapter) extends HCompPortalAdapter {
	private var spentCents = 0d

	override def sendQuery(query: HCompQuery, properties: HCompQueryProperties): Future[Option[HCompAnswer]] = {
		decoratedPortal.synchronized {
			spentCents += properties.paymentCents
		}
		decoratedPortal.sendQuery(query, properties)
	}

	def cost = spentCents

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] =
		decoratedPortal.processQuery(query, properties)

	override def getDefaultPortalKey: Symbol = decoratedPortal.getDefaultPortalKey
}

case class HCompQueryStats(query: HCompQuery, answer: Option[HCompAnswer], timeMillis: Long, moneySpent: Double)

private object HCompIDGen {
	private val current = new AtomicInteger(0)

	def next() = current.incrementAndGet()
}

trait HCompQuery {
	def question: String

	def title: String

	final val identifier: Int = HCompIDGen.next()
}

trait HCompAnswer {
	def query: HCompQuery
}

trait HCompInstructions {
	def toString: String
}

case class HCompInstructionsWithTuple(questionBeforeTuples: String, questionBetweenTuples: String = "", questionAfterTuples: String = "") {
	def getInstructions(data1: String, data2: String = "") = <div>
		<p>
			{questionBeforeTuples}
		</p>
		<p>
			<i>
				{data1}
			</i>
		</p>{if (questionBetweenTuples != "") <p>
			{questionBetweenTuples}
		</p>}{if (data2 == "") <p>
			<i>
				{data2}
			</i>
		</p>}{if (questionAfterTuples != "") <p>
			{questionAfterTuples}
		</p>}
	</div>.toString
}

object HCompConversions {
	implicit def hcompInstrToString(instr: HCompInstructions): String = instr.toString
}

case class CompositeQuery(queries: List[HCompQuery], question: String = "", title: String = "") extends HCompQuery {
	def this(queries: List[HCompQuery], question: String) = this(queries, question, question)
}

object CompositeQuery {
	def apply(queries: List[HCompQuery], question: String): CompositeQuery = apply(queries, question, question)
}

case class CompositeQueryAnswer(query: CompositeQuery, answers: Map[HCompQuery, Option[HCompAnswer]]) extends HCompAnswer {
	def get[T](query: HCompQuery): T = answers(query).get.asInstanceOf[T]

	def getByTitle[T](title: String): Option[T] = {
		answers.keys.find(_.title == title) match {
			case Some(k) => Some(answers(k).get.asInstanceOf[T])
			case None => None
		}
	}

	override def toString() = answers.map(q => q._1.question + "::" + q._2.getOrElse("[no answer]")).mkString("\n")
}

/**
 * @param question
 * @param defaultAnswer
 * @param title
 */
case class FreetextQuery(question: String, defaultAnswer: String = "", title: String = "") extends HCompQuery {
	def this(question: String, defaultAnswer: String) = this(question, defaultAnswer, question)

	def this(question: String) = this(question, "", question)

	var valueIsRequired: Boolean = defaultAnswer.equals("")

	def setRequired(required: Boolean) = {
		valueIsRequired = required;
		this
	}
}

object FreetextQuery {
	def apply(question: String, defaultAnswer: String): FreetextQuery = apply(question, defaultAnswer, question)

	def apply(question: String): FreetextQuery = apply(question, "", question)
}

case class FreetextAnswer(query: FreetextQuery, answer: String) extends HCompAnswer {
	override def toString() = answer
}

case class MultipleChoiceQuery(question: String, options: List[String], maxNumberOfResults: Int, minNumberOfResults: Int = 1, title: String = "") extends HCompQuery {
	def this(question: String, options: List[String], maxNumberOfResults: Int) = this(question, options, maxNumberOfResults, 1, question)
}

object MultipleChoiceQuery {
	def apply(question: String, options: List[String], maxNumberOfResults: Int): MultipleChoiceQuery = apply(question, options, maxNumberOfResults, title = question)
}

case class MultipleChoiceAnswer(query: MultipleChoiceQuery, answer: Map[String, Boolean]) extends HCompAnswer {
	def selectedAnswers: List[String] = answer.collect({
		case x if x._2 => x._1
	}).toList

	def selectedAnswer = selectedAnswers(0)

	override def toString() = selectedAnswers.mkString(", ")
}

case class HCompException(query: HCompQuery, exception: Throwable) extends HCompAnswer

case class HCompQueryProperties(paymentCents: Double = 1d)