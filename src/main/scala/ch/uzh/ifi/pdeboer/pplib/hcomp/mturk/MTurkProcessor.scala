package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.{GrowingTimer, U}
import com.amazonaws.mturk.addon.BatchItemCallback
import com.amazonaws.mturk.requester.{HIT, Assignment}
import com.amazonaws.mturk.service.axis.RequesterService

import scala.concurrent.duration._
import scala.xml.{XML, Source, NodeSeq}

/**
 * Created by pdeboer on 19/11/14.
 */
class MTurkManager(val service: RequesterService, val query: HCompQuery, val properties: HCompQueryProperties) {
	var hit: HIT = null
	var cancelled: Boolean = false

	private val emptyCallback: BatchItemCallback with Object {def processItemResult(o: Any, b: Boolean, o1: Any, e: Exception): Unit} = new BatchItemCallback {
		override def processItemResult(o: scala.Any, b: Boolean, o1: scala.Any, e: Exception): Unit = {}
	}

	def waitForResponse(): Option[HCompAnswer] = {
		val timer = new GrowingTimer(1 second, 1.1, 30 seconds)
		U.retry(100000) {
			//at least 27h
			if (!cancelled) {
				timer.waitTime

				val answer = poll()
				if (answer == None) throw new IllegalStateException("let's wait some more")
				answer
			} else None
		}
	}

	def cancelHIT(): Unit = {
		cancelled = true
		service.deleteHITs(Array(hit.getHITId), true, true, emptyCallback)
	}

	/**
	 * @return HIT ID
	 */
	def createHIT(): HIT = {
		val mtQuery = MTQuery.convert(query)

		val hit = service.createHIT(query.title, query.question, properties.paymentCents, mtQuery.xml, 1, true)
		hit
	}

	def poll(): Option[HCompAnswer] = {
		val assignments = service.getAllAssignmentsForHIT(hit.getHITId)
		assignments.headOption match {
			case None => None
			case Some(a: Assignment) => {
				handleAssignmentResult(a)
			}
		}
	}

	def handleAssignmentResult(a: Assignment): Some[HCompAnswer] = {
		//We approve all assignments by default. Don't like rejections
		service.approveAssignments(Array(a.getAssignmentId), Array("Thanks for your work"), "Thanks for your work", emptyCallback)

		val xml = XML.loadString(a.getAnswer)
		Some(MTQuery.convert(query).interpret(xml))
	}

	def hitXML(question: NodeSeq) =
		<QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
			{question}
		</QuestionForm>
}

sealed trait MTQuery {
	def id: String = "query" + rawQuery.identifier

	def defaultXML(title: String = rawQuery.title, question: String = rawQuery.question, injectedXML: NodeSeq = elementXML, valueIsRequired: Boolean = true) =
		<Question>
			<QuestionIdentifier>
				{id}
			</QuestionIdentifier>
			<DisplayName>
				{title}
			</DisplayName>
			<isRequired>
				{valueIsRequired}
			</isRequired>
			<QuestionContent>
				<FormattedContent>
					{question}
				</FormattedContent>
			</QuestionContent>
			<AnswerSpecification>
				{injectedXML}
			</AnswerSpecification>
		</Question>.toString

	def xml: String = defaultXML()

	def getRelevantAnswerFromResponseXML(response: NodeSeq) = (response \\ "Answer").filter(e => (e \ "QuestionIdentifier").text == id)

	def elementXML: NodeSeq

	def rawQuery: HCompQuery

	def interpret(xml: NodeSeq): HCompAnswer
}

object MTQuery {
	def convert(query: HCompQuery) = query match {
		case q: FreetextQuery => new MTFreeTextQuery(q)
		case q: MultipleChoiceQuery => new MTMultipleChoiceQuery(q)
		case q: CompositeQuery => new MTCompositeQuery(q)
	}
}

class MTFreeTextQuery(val rawQuery: FreetextQuery) extends MTQuery {
	def elementXML: NodeSeq = {
		<FreeTextAnswer>
			{if (rawQuery.defaultAnswer != "") <DefaultText>
			{rawQuery.defaultAnswer}
		</DefaultText>}
		</FreeTextAnswer>
	}

	override def interpret(xml: NodeSeq): HCompAnswer =
		FreetextAnswer(rawQuery, (getRelevantAnswerFromResponseXML(xml) \ "FreeText").text)
}

class MTMultipleChoiceQuery(val rawQuery: MultipleChoiceQuery) extends MTQuery {
	def elementXML: NodeSeq = {
		<SelectionAnswer>
			<MinSelectionCount>
				{rawQuery.minNumberOfResults}
			</MinSelectionCount>
			<MaxSelectionCount>
				{rawQuery.maxSelections}
			</MaxSelectionCount>
		</SelectionAnswer>
	}

	override def interpret(xml: NodeSeq): HCompAnswer = {
		val selected = (getRelevantAnswerFromResponseXML(xml) \\ "SelectionIdentifier").map(i => i.text).toSet
		MultipleChoiceAnswer(rawQuery, rawQuery.options.map(o => o -> selected.contains(o)).toMap)
	}
}

class MTCompositeQuery(val rawQuery: CompositeQuery) extends MTQuery {
	override def elementXML: NodeSeq = {
		//simple way to get a nodeseq out of a list
		<el>
			{rawQuery.queries.map(MTQuery.convert(_).elementXML)}
		</el> \ "el"
	}

	override def interpret(xml: NodeSeq): HCompAnswer = {
		CompositeQueryAnswer(rawQuery,
			rawQuery.queries.map(q => q -> Some(MTQuery.convert(q).interpret(xml))).toMap)
	}
}