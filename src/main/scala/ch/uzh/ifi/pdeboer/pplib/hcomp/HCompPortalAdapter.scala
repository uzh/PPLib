package ch.uzh.ifi.pdeboer.pplib.hcomp

import java.util.concurrent.atomic.AtomicInteger

import ch.uzh.ifi.pdeboer.pplib.hcomp.QualificationType.{QTLocale, QTNumberHITsApproved, QTPercentAssignmentsRejected}
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.Prunable
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

	def sendQuery(query: HCompQuery, details: HCompQueryProperties = HCompQueryProperties(), omitBudgetCalculation: Boolean = false): Future[Option[HCompAnswer]] = Future {
		sendQueryNoFuture(query, details, omitBudgetCalculation)
	}

	def sendQueryNoFuture(query: HCompQuery, details: HCompQueryProperties = HCompQueryProperties(), omitBudgetCalculation: Boolean = false): Option[HCompAnswer] = {
		val properties = if (details.paymentCents < 1) HCompQueryProperties(query.suggestedPaymentCents) else details

		val budgetAfterQuery = if (omitBudgetCalculation) budget
		else budget match {
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
			logger.debug(s"sending query $query with properties $properties . Budget after query $budget")
			val timeBefore = new DateTime()

			val answer = query.answerTrivialCases match {
				case Some(x) => Some(x)
				case None => processQuery(query, properties)
			}

			val timeAfter = new DateTime()
			val durationMillis = timeAfter.getMillis - timeBefore.getMillis
			logger.debug(s"got answer for query $query after $durationMillis ms. Answer = $answer")

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
		val future: Future[Option[HCompAnswer]] = sendQuery(query, properties)
		Await.result(future, 1 day)
		future.value.get.get
		/*
		val (future, cancelQueryFuture) = U.interruptableFuture[Option[HCompAnswer]] { () =>
			sendQueryNoFuture(query, properties)
		}

		val timeAtMaxWait = new DateTime(DateTime.now().getMillis + maxWaitTime.toMillis)
		if (maxWaitTime.toMillis <= 0) None
		else {
			val waitStep = Math.min(properties.cancelAndRepostAfter.toMillis, maxWaitTime.toMillis)
			try {
				Await.result(future, waitStep milliseconds)
				future.value.get.get
			}
			catch {
				case e: TimeoutException =>
					if (timeAtMaxWait.isAfterNow) {
						logger.info(s"query with identifier ${query.identifier} timed out. Reposting..")
						cancelQueryFuture()
						cancelQuery(query)
						sendQueryAndAwaitResult(query, properties,
							maxWaitTime = (timeAtMaxWait.getMillis - DateTime.now().getMillis) millis)
					} else None
			}
		}
		*/
	}

	def getDefaultPortalKey: String

	def queries = queryLog

	def cancelQuery(query: HCompQuery): Unit
}


class CostCountingEnabledHCompPortal(val decoratedPortal: HCompPortalAdapter) extends HCompPortalAdapter {
	private var spentCents = 0d
	private var spentPerQuery = scala.collection.mutable.HashMap.empty[Int, Double]

	override def sendQuery(query: HCompQuery, properties: HCompQueryProperties = HCompQueryProperties(), omitBudgetCalculation: Boolean = false): Future[Option[HCompAnswer]] = {
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
			try {
				spentCents -= spentPerQuery(query.identifier)
			}
			catch {
				case e: Throwable => logger.info(s"couldn't find query with ID ${query.identifier}. Did not deduce it's money", e)
			}
		}
	}
}

case class HCompQueryStats(query: HCompQuery, answer: Option[HCompAnswer], timeMillis: Long, moneySpent: Double)

private object HCompIDGen {
	private val current = new AtomicInteger(0)

	def next() = current.incrementAndGet()
}

@SerialVersionUID(1l)
trait HCompQuery extends Serializable {
	def question: String

	def title: String

	def suggestedPaymentCents: Int

	final val identifier: Int = HCompIDGen.next()

	def answerTrivialCases: Option[HCompAnswer] = None

	def valueIsRequired: Boolean = true
}

@SerialVersionUID(1l)
trait HCompAnswer extends Serializable with Prunable {
	def query: HCompQuery

	def is[T]: T = this.asInstanceOf[T]

	var postTime: DateTime = null
	var acceptTime: Option[DateTime] = None
	var submitTime: Option[DateTime] = None
	var receivedTime: DateTime = null

	def processingTimeMillis: Long = submitTime.getOrElse(receivedTime).getMillis - acceptTime.getOrElse(postTime).getMillis

	override def prunableDouble = processingTimeMillis.toDouble
}

@SerialVersionUID(1l)
class HCompInstructionsWithTuple(val _questionBeforeTuples: NodeSeq, val _questionBetweenTuples: NodeSeq = NodeSeq.fromSeq(Nil), val _questionAfterTuples: NodeSeq = NodeSeq.fromSeq(Nil), val _enableSecondDataFieldIfAvailable: Boolean = true) extends Serializable {
	def getInstructions(data1: String, data2: String = "", htmlData: NodeSeq = Nil) =
		NodeSeq.fromSeq(<placeholder>
			<p>
				{_questionBeforeTuples}
			</p>{if (data1 != "")
				<p>
					<i>
						{data1}
					</i>
				</p>}{if (_questionBetweenTuples.length > 0) _questionBetweenTuples}{if (data2 != "" && _enableSecondDataFieldIfAvailable) <p>
				<i>
					{data2}
				</i>
			</p>}{if (_questionAfterTuples.length > 0) <p>
				{_questionAfterTuples}
			</p>}{htmlData}
		</placeholder>
			.child).toString

	//TODO write function that flattens "P"'s
}

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompInstructionsWithTupleStringified._

@SerialVersionUID(1l)
case class HCompInstructionsWithTupleStringified(questionBeforeTuples: String, questionBetweenTuples: String = "", questionAfterTuples: String = "", enableSecondDataFieldIfAvailable: Boolean = true) extends HCompInstructionsWithTuple(
	prep(questionBeforeTuples), if (questionBetweenTuples == "") Nil
	else <p>
		{questionBetweenTuples}
	</p>, prep(questionAfterTuples), enableSecondDataFieldIfAvailable) with Serializable {
}

object HCompInstructionsWithTupleStringified {
	def prep(str: String): NodeSeq = if (U.removeWhitespaces(str) == "") NodeSeq.fromSeq(Nil)
	else <p>
		{str}
	</p>.child
}

@SerialVersionUID(1l)
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
@SerialVersionUID(1l)
case class FreetextQuery(question: String, defaultAnswer: String = "", title: String = "") extends HCompQuery with Serializable {
	def this(question: String, defaultAnswer: String) = this(question, defaultAnswer, question)

	def this(question: String) = this(question, "", question)

	private var _valueIsRequired: Boolean = defaultAnswer.equals("")

	override def valueIsRequired = _valueIsRequired

	def setRequired(required: Boolean) = {
		_valueIsRequired = required
		this
	}

	override def suggestedPaymentCents: Int = 8
}

object FreetextQuery {
	def apply(question: String, defaultAnswer: String): FreetextQuery = apply(question, defaultAnswer, question)

	def apply(question: String): FreetextQuery = apply(question, "", question)
}

@SerialVersionUID(1l)
case class FreetextAnswer(query: FreetextQuery, answer: String) extends HCompAnswer with Serializable {
	override def toString() = answer
}

@SerialVersionUID(1l)
case class MultipleChoiceQuery(question: String, private val _options: List[String], maxNumberOfResults: Int, minNumberOfResults: Int = 1, title: String = "", override val valueIsRequired: Boolean = true) extends HCompQuery with Serializable {
	val options = _options.distinct
	assert(maxNumberOfResults < 1 || maxNumberOfResults >= minNumberOfResults)

	def this(question: String, options: List[String], maxNumberOfResults: Int) = this(question, options, maxNumberOfResults, 1, question)

	def maxSelections = if (maxNumberOfResults < 1) options.size else maxNumberOfResults

	override def answerTrivialCases: Option[HCompAnswer] =
		if (options.size == 1 && minNumberOfResults == 1) Some(new MultipleChoiceAnswer(this, Map(options(0) -> true)))
		else None

	override def suggestedPaymentCents: Int = 4
}

object MultipleChoiceQuery {
	def apply(question: String, options: List[String], maxNumberOfResults: Int): MultipleChoiceQuery = apply(question, options, maxNumberOfResults, title = question)
}

@SerialVersionUID(1l)
case class MultipleChoiceAnswer(query: MultipleChoiceQuery, answer: Map[String, Boolean]) extends HCompAnswer with Serializable {
	def selectedAnswers: List[String] = answer.collect({
		case x if x._2 => x._1
	}).toList

	def selectedAnswer = selectedAnswers(0)

	override def toString() = selectedAnswers.mkString(", ")
}

@SerialVersionUID(1l)
case class HCompException(query: HCompQuery, exception: Throwable) extends HCompAnswer with Serializable

@SerialVersionUID(1l)
case class HCompJobCancelled(query: HCompQuery) extends HCompAnswer with Serializable

@SerialVersionUID(1l)
case class HCompQueryProperties(paymentCents: Int = 0, cancelAndRepostAfter: Duration = 1 day, qualifications: List[QueryWorkerQualification] = HCompQueryProperties.DEFAULT_QUALIFICATIONS) extends Serializable

object HCompQueryProperties {
	val DEFAULT_QUALIFICATIONS = List(new QTLocale === "US", new QTPercentAssignmentsRejected < 4, new QTNumberHITsApproved > 4000)
}

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