package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.xml.{Node, NodeSeq}

sealed trait MTQuery {
	def id: String = "query" + rawQuery.identifier

	def defaultXML(title: String = rawQuery.title, question: String = rawQuery.question, injectedXML: Node = elementXML, valueIsRequired: Boolean = true) =
		<QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
		<Question>
			<QuestionIdentifier>
				{id}
			</QuestionIdentifier>
			<DisplayName>
				{title}
			</DisplayName>
			<IsRequired>
				{valueIsRequired}
			</IsRequired>
			<QuestionContent>
				<FormattedContent>
					{scala.xml.PCData(question)}
				</FormattedContent>
			</QuestionContent>
			<AnswerSpecification>
				{injectedXML}
			</AnswerSpecification>
		</Question>
		</QuestionForm>.theSeq(0)

	def xml = defaultXML()

	def getRelevantAnswerFromResponseXML(response: NodeSeq) =
		(response \\ "Answer").filter(e => U.removeWhitespaces((e \ "QuestionIdentifier").text) == id)

	def elementXML: Node

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
	def elementXML: Node = {
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
	def elementXML: Node = {
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
	override def elementXML: Node = {
		//simple way to get a nodeseq out of a list
		<el>
			{rawQuery.queries.map(MTQuery.convert(_).elementXML)}
		</el>.theSeq(0)
	}

	override def interpret(xml: NodeSeq): HCompAnswer = {
		CompositeQueryAnswer(rawQuery,
			rawQuery.queries.map(q => q -> Some(MTQuery.convert(q).interpret(xml))).toMap)
	}
}