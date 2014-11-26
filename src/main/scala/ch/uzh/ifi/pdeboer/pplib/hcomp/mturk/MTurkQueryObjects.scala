package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.xml._

sealed trait MTQuery {
	def id: String = "query" + rawQuery.identifier

	def xml = scala.xml.Utility.trim(<QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
		{questionXML}
	</QuestionForm>).theSeq(0)

	def questionXML: NodeSeq

	def defaultQuestionXML(injectedXML: NodeSeq, title: String = rawQuery.title, question: String = rawQuery.question, valueIsRequired: Boolean = true) =
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

	def getRelevantAnswerFromResponseXML(response: NodeSeq) =
		(response \\ "Answer").filter(e => U.removeWhitespaces((e \ "QuestionIdentifier").text) == id)

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
	def answerSpecification: Node = {
		<FreeTextAnswer>
			{if (rawQuery.defaultAnswer != "") <DefaultText>
			{rawQuery.defaultAnswer}
		</DefaultText>}
		</FreeTextAnswer>
	}

	override def interpret(xml: NodeSeq): HCompAnswer =
		FreetextAnswer(rawQuery, (getRelevantAnswerFromResponseXML(xml) \ "FreeText").text)

	override def questionXML: NodeSeq = defaultQuestionXML(injectedXML = answerSpecification)
}

class MTMultipleChoiceQuery(val rawQuery: MultipleChoiceQuery) extends MTQuery {
	def answerSpecification: NodeSeq = {
		<SelectionAnswer>
			<MinSelectionCount>
				{rawQuery.minNumberOfResults}
			</MinSelectionCount>
			<MaxSelectionCount>
				{rawQuery.maxSelections}
			</MaxSelectionCount>
			<StyleSuggestion>
				{if (rawQuery.maxSelections == 1) "radiobutton" else "checkbox"}
			</StyleSuggestion>
			<Selections>
				{rawQuery.options.zipWithIndex.map(o => {
				<Selection>
					<SelectionIdentifier>
						{id + "_" + o._2}
					</SelectionIdentifier>
					<Text>
						{o._1}
					</Text>
				</Selection>
			})}
			</Selections>
		</SelectionAnswer>
	}

	override def interpret(xml: NodeSeq): HCompAnswer = {
		val selected = (getRelevantAnswerFromResponseXML(xml) \\ "SelectionIdentifier").map(i => {
			val selectionId: String = i.text.split("_")(1)
			val cleanedSelectionId: String = selectionId.replaceAll("[^0-9]", "")
			if (cleanedSelectionId == "") -1 else cleanedSelectionId.toInt
		}).toSet
		MultipleChoiceAnswer(rawQuery, rawQuery.options.zipWithIndex.map(o => o._1 -> selected.contains(o._2)).toMap)
	}

	override def questionXML: NodeSeq = defaultQuestionXML(answerSpecification)
}

class MTCompositeQuery(val rawQuery: CompositeQuery) extends MTQuery {

	override def interpret(xml: NodeSeq): HCompAnswer = {
		CompositeQueryAnswer(rawQuery,
			rawQuery.queries.map(q => q -> Some(MTQuery.convert(q).interpret(xml))).toMap)
	}

	override def questionXML: NodeSeq =
		NodeSeq.fromSeq(rawQuery.queries.map(MTQuery.convert(_).questionXML.theSeq).flatten)
}