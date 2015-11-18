package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}

import scala.xml._

sealed trait MTQuery extends LazyLogger {
	def id: String = "query" + rawQuery.identifier

	def xml = scala.xml.Utility.trim(<QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
		{questionXML}
	</QuestionForm>).theSeq(0)

	def questionXML: NodeSeq

	def defaultQuestionXML(injectedXML: NodeSeq, title: String = rawQuery.title, question: String = rawQuery.question, valueIsRequired: Boolean = rawQuery.valueIsRequired) =
		<Question>
			<QuestionIdentifier>
				{id}
			</QuestionIdentifier>
			<DisplayName>
				{title.take(120)}
			</DisplayName>
			<IsRequired>
				{valueIsRequired}
			</IsRequired>
			<QuestionContent>
				<FormattedContent>
					{PCData(question)}
				</FormattedContent>
			</QuestionContent>
			<AnswerSpecification>
				{injectedXML}
			</AnswerSpecification>
		</Question>

	def getRelevantAnswerFromResponseXML(response: NodeSeq) =
		(response \\ "Answer").filter(e => U.removeWhitespaces((e \ "QuestionIdentifier").text) == id)

	def rawQuery: HCompQuery

	def interpret(xml: NodeSeq, workerId: String): Option[HCompAnswer]
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

	override def interpret(xml: NodeSeq, workerId: String) =
		Some(FreetextAnswer(rawQuery, (getRelevantAnswerFromResponseXML(xml) \ "FreeText").text, responsibleWorkers = List(MTurkWorker(workerId))))

	override def questionXML: NodeSeq = defaultQuestionXML(injectedXML = answerSpecification)
}


class MTExternalQuery(val rawQuery: ExternalQuery) extends MTQuery {

	override def interpret(xml: NodeSeq, workerId: String) = ???


	override def questionXML: NodeSeq = <ExternalQuestion>
		<ExternalURL>
			{rawQuery.url}
		</ExternalURL>
		<FrameHeight>800</FrameHeight>
	</ExternalQuestion>
}

class MTMultipleChoiceQuery(val rawQuery: MultipleChoiceQuery) extends MTQuery {
	val processedOptions = rawQuery.options.map(_.replaceAll("\\n", ""))

	def answerSpecification: NodeSeq = {
		<SelectionAnswer>
			<MinSelectionCount>
				{rawQuery.minNumberOfResults}
			</MinSelectionCount>
			<MaxSelectionCount>
				{rawQuery.maxSelections}
			</MaxSelectionCount>
			<StyleSuggestion>
				{if (rawQuery.maxSelections == 1 && rawQuery.minNumberOfResults == 1) "radiobutton" else "checkbox"}
			</StyleSuggestion>
			<Selections>
				{processedOptions.zipWithIndex.map(o => {
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

	override def interpret(xml: NodeSeq, workerId: String): Option[HCompAnswer] = {
		val relevantXMLAnswers: NodeSeq = getRelevantAnswerFromResponseXML(xml) \\ "SelectionIdentifier"
		val selected = relevantXMLAnswers.map(i => {
			val selectionId: String = i.text.split("_")(1)
			val cleanedSelectionId: String = selectionId.replaceAll("[^0-9]*", "")
			if (cleanedSelectionId == "") {
				logger.error(s"cleaned section ID part and nothing was left. original $i, splitted ${selectionId.mkString(",")}")
				-1
			} else cleanedSelectionId.toInt
		}).toSet
		val selections: Map[String, Boolean] = rawQuery.options.zipWithIndex.map(o => o._1 -> selected.contains(o._2)).toMap
		assert(relevantXMLAnswers.size == selections.values.count(_ == true))
		val answer = MultipleChoiceAnswer(rawQuery, selections, responsibleWorkers = List(MTurkWorker(workerId)))
		if (rawQuery.minNumberOfResults > 0 && answer.selectedAnswers.isEmpty) {
			logger.error("expected result for multiple choice query, but got none. Here's the complete answer: " + xml.toString())
			None
		} else Some(answer)
	}

	override def questionXML: NodeSeq = defaultQuestionXML(answerSpecification)
}

class MTCompositeQuery(val rawQuery: CompositeQuery) extends MTQuery {

	override def interpret(xml: NodeSeq, workerId: String) = {
		Some(CompositeQueryAnswer(rawQuery,
			rawQuery.queries.map(q => q -> MTQuery.convert(q).interpret(xml, workerId)).toMap))
	}

	override def questionXML: NodeSeq =
		NodeSeq.fromSeq(rawQuery.queries.map(MTQuery.convert(_).questionXML.theSeq).flatten)
}