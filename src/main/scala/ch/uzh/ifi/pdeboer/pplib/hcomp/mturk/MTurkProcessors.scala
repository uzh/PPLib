package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import com.amazonaws.mturk.service.axis.RequesterService

import scala.xml.{NodeSeq, Elem, Node}

/**
 * Created by pdeboer on 19/11/14.
 */
class MTurkManager(val service: RequesterService) {
	def sendQueryAndWaitForResponse(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		val mtQuery = MTQuery.convert(query)

		???
	}
}

sealed trait MTQuery {
	def defaultXML(title: String = rawQuery.title, question: String = rawQuery.question, injectedXML: NodeSeq = elementXML, valueIsRequired: Boolean = true) =
		<Question>
			<QuestionIdentifier>query
				{rawQuery.identifier}
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

	def elementXML: NodeSeq

	def rawQuery: HCompQuery
}

object MTQuery {
	def convert(query: HCompQuery) = query match {
		case q: FreetextQuery => new MTFreeTextQuery(q)
		case q: MultipleChoiceQuery => new MTMultipleChoiceQuery(q)
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
}

class MTCompositeQuery(val rawQuery: CompositeQuery) extends MTQuery {
	override def elementXML: NodeSeq = <el>
		{rawQuery.queries.map(MTQuery.convert(_).elementXML)}
	</el> \ "el"
}