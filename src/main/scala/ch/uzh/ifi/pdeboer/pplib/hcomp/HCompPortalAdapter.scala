package ch.uzh.ifi.pdeboer.pplib.hcomp

import java.util.concurrent.atomic.AtomicInteger

import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}
import com.typesafe.config.Config
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, _}
import scala.xml.NodeSeq


/**
 * Created by pdeboer on 10/10/14.
 */

trait HCompPortalAdapter extends LazyLogger {
	private var _budget: Option[Int] = None

	def setBudget(budget: Option[Int]): Unit =
		synchronized {
			_budget = budget
		}

	def budget = _budget

	//TODO we should hide this method somehow to the public
	def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer]

	private var queryLog = List.empty[HCompQueryStats]

	def sendQuery(query: HCompQuery, details: HCompQueryProperties = HCompQueryProperties()): Future[Option[HCompAnswer]] = Future {
		val properties = if (details.paymentCents < 1) HCompQueryProperties(query.suggestedPaymentCents) else details
		
		val budgetAfterQuery = budget match {
			case Some(x) => Some(x - properties.paymentCents)
			case None => None
		}
		if (budgetAfterQuery.isDefined && budgetAfterQuery.get < 0) {
			logger.error("rejected query due to insufficient funds in budget")
			None
		} else {
			synchronized {
				_budget = budgetAfterQuery
			}
			logger.info(s"sending query $query with properties $properties . Budget after query $budget")
			val timeBefore = new DateTime()

			val answer = query.answerTrivialCases match {
				case Some(x) => Some(x)
				case None => processQuery(query, properties)
			}

			val timeAfter = new DateTime()
			val durationMillis = timeAfter.getMillis - timeBefore.getMillis
			logger.info(s"got answer for query $query after $durationMillis ms. Answer = $answer")

			answer match {
				case Some(x: HCompAnswer) => {
					x.postTime = timeBefore
					x.receivedTime = timeAfter
				}
			}

			queryLog = HCompQueryStats(query, answer, durationMillis, properties.paymentCents) :: queryLog

			answer
		}
	}

	def sendQueryAndAwaitResult(query: HCompQuery, properties: HCompQueryProperties = HCompQueryProperties(), maxWaitTime: Duration = 14 days): Option[HCompAnswer] = {
		val future = sendQuery(query, properties)
		Await.result(future, maxWaitTime)
		future.value.get.get
	}

	def getDefaultPortalKey: String

	def queries = queryLog

	def cancelQuery(query: HCompQuery): Unit
}

class CostCountingEnabledHCompPortal(decoratedPortal: HCompPortalAdapter) extends HCompPortalAdapter {
	private var spentCents = 0d
	private var spentPerQuery = scala.collection.mutable.HashMap.empty[Int, Double]

	override def sendQuery(query: HCompQuery, properties: HCompQueryProperties): Future[Option[HCompAnswer]] = {
		decoratedPortal.synchronized {
			spentCents += properties.paymentCents
			spentPerQuery += query.identifier -> properties.paymentCents
		}
		decoratedPortal.sendQuery(query, properties)
	}

	def cost = spentCents

	def costPerQuery = spentPerQuery.toMap

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] =
		decoratedPortal.processQuery(query, properties)

	override def getDefaultPortalKey: String = decoratedPortal.getDefaultPortalKey

	override def cancelQuery(query: HCompQuery): Unit = {
		decoratedPortal.cancelQuery(query)
		decoratedPortal.synchronized {
			spentCents -= spentPerQuery(query.identifier)
		}
	}
}

case class HCompQueryStats(query: HCompQuery, answer: Option[HCompAnswer], timeMillis: Long, moneySpent: Double)

private object HCompIDGen {
	private val current = new AtomicInteger(0)

	def next() = current.incrementAndGet()
}

trait HCompQuery extends Serializable {
	def question: String

	def title: String

	def suggestedPaymentCents: Int

	final val identifier: Int = HCompIDGen.next()

	def answerTrivialCases: Option[HCompAnswer] = None
}

trait HCompAnswer extends Serializable {
	def query: HCompQuery

	def as[T]: T = this.asInstanceOf[T]

	var postTime: DateTime = null
	var acceptTime: Option[DateTime] = None
	var submitTime: Option[DateTime] = None
	var receivedTime: DateTime = null

	def processingTimeMillis: Long = submitTime.getOrElse(receivedTime).getMillis - acceptTime.getOrElse(postTime).getMillis
}

case class HCompInstructionsWithTuple(questionBeforeTuples: String, questionBetweenTuples: String = "", questionAfterTuples: String = "", enableSecondDataFieldIfAvailable: Boolean = true) extends Serializable {
	def getInstructions(data1: String, data2: String = "") =
		NodeSeq.fromSeq(<placeholder>
			<p>
				{questionBeforeTuples}
			</p>
			<p>
				<i>
					{data1}
				</i>
			</p>{if (questionBetweenTuples != "") <p>
				{questionBetweenTuples}
			</p>}{if (data2 != "" && enableSecondDataFieldIfAvailable) <p>
				<i>
					{data2}
				</i>
			</p>}{if (questionAfterTuples != "") <p>
				{questionAfterTuples}
			</p>}
		</placeholder>
			.child).toString
}

case class CompositeQuery(queries: List[HCompQuery], question: String = "", title: String = "") extends HCompQuery with Serializable {
	def this(queries: List[HCompQuery], question: String) = this(queries, question, question)

	override def suggestedPaymentCents: Int = queries.map(_.suggestedPaymentCents).sum
}

object CompositeQuery {
	def apply(queries: List[HCompQuery], question: String): CompositeQuery = apply(queries, question, question)
}

case class CompositeQueryAnswer(query: CompositeQuery, answers: Map[HCompQuery, Option[HCompAnswer]]) extends HCompAnswer with Serializable {
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
case class FreetextQuery(question: String, defaultAnswer: String = "", title: String = "") extends HCompQuery with Serializable {
	def this(question: String, defaultAnswer: String) = this(question, defaultAnswer, question)

	def this(question: String) = this(question, "", question)

	var valueIsRequired: Boolean = defaultAnswer.equals("")

	def setRequired(required: Boolean) = {
		valueIsRequired = required;
		this
	}

	override def suggestedPaymentCents: Int = 6
}

object FreetextQuery {
	def apply(question: String, defaultAnswer: String): FreetextQuery = apply(question, defaultAnswer, question)

	def apply(question: String): FreetextQuery = apply(question, "", question)
}

case class FreetextAnswer(query: FreetextQuery, answer: String) extends HCompAnswer with Serializable {
	override def toString() = answer
}

case class MultipleChoiceQuery(question: String, options: List[String], maxNumberOfResults: Int, minNumberOfResults: Int = 1, title: String = "") extends HCompQuery with Serializable {
	assert(maxNumberOfResults < 1 || maxNumberOfResults >= minNumberOfResults)

	def this(question: String, options: List[String], maxNumberOfResults: Int) = this(question, options, maxNumberOfResults, 1, question)

	def maxSelections = if (maxNumberOfResults < 1) options.length else maxNumberOfResults

	override def answerTrivialCases: Option[HCompAnswer] =
		if (options.size == 1 && minNumberOfResults == 1) Some(new MultipleChoiceAnswer(this, Map(options(0) -> true)))
		else None

	override def suggestedPaymentCents: Int = 3
}

object MultipleChoiceQuery {
	def apply(question: String, options: List[String], maxNumberOfResults: Int): MultipleChoiceQuery = apply(question, options, maxNumberOfResults, title = question)
}

case class MultipleChoiceAnswer(query: MultipleChoiceQuery, answer: Map[String, Boolean]) extends HCompAnswer with Serializable {
	def selectedAnswers: List[String] = answer.collect({
		case x if x._2 => x._1
	}).toList

	def selectedAnswer = selectedAnswers(0)

	override def toString() = selectedAnswers.mkString(", ")
}

case class HCompException(query: HCompQuery, exception: Throwable) extends HCompAnswer with Serializable

case class HCompJobCancelled(query: HCompQuery) extends HCompAnswer with Serializable

case class HCompQueryProperties(paymentCents: Int = 0) extends Serializable

trait HCompPortalBuilder {
	private var _params = collection.mutable.HashMap.empty[String, String]

	def loadConfig(config: Config): Unit = {
		parameterToConfigPath.foreach {
			case (parameter, configPath) =>
				U.getConfigString(configPath) match {
					case Some(configValue) => setParameter(parameter, configValue)
					case _ => {}
				}
		}
	}

	def build: HCompPortalAdapter

	def params: Map[String, String] = _params.toMap

	def expectedParameters: List[String]

	def parameterToConfigPath: Map[String, String]

	def setParameter(param: String, value: String): Unit = _params += param -> value
}