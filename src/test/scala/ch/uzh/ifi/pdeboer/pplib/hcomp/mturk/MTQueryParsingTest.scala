package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextAnswer, FreetextQuery, MultipleChoiceAnswer, MultipleChoiceQuery}
import ch.uzh.ifi.pdeboer.pplib.util.U._
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 20/11/14.
 */
class MTQueryParsingTest {
	@Test
	def testRemoveWhitespaces: Unit = {
		Assert.assertEquals("addfs", removeWhitespaces("a d	d\nf s"))
	}

	@Test
	def testInterpretationTextfield: Unit = {
		val ftq: FreetextQuery = FreetextQuery("question")
		val q = MTQuery.convert(ftq)

		//Taken from http://docs.aws.amazon.com/AWSMechTurk/latest/AWSMturkAPI/ApiReference_QuestionFormAnswersDataStructureArticle.html
		val xml = <QuestionFormAnswers xmlns="[the QuestionFormAnswers schema URL]">
			<Answer>
				<QuestionIdentifier>
					{q.id}
				</QuestionIdentifier>
				<FreeText>C3</FreeText>
			</Answer>
			<Answer>
				<QuestionIdentifier>likelytowin</QuestionIdentifier>
				<SelectionIdentifier>notlikely</SelectionIdentifier>
			</Answer>
		</QuestionFormAnswers>

		Assert.assertEquals(Some(FreetextAnswer(ftq, "C3")), q.interpret(xml))
	}

	@Test
	def testInterpretationMultipleChoice: Unit = {
		val mtc = MultipleChoiceQuery("q", List("a", "b", "c"), 1)
		val q = MTQuery.convert(mtc)

		//Taken from http://docs.aws.amazon.com/AWSMechTurk/latest/AWSMturkAPI/ApiReference_QuestionFormAnswersDataStructureArticle.html
		val xml = <QuestionFormAnswers xmlns="[the QuestionFormAnswers schema URL]">
			<Answer>
				<QuestionIdentifier>asdf</QuestionIdentifier>
				<FreeText>C3</FreeText>
			</Answer>
			<Answer>
				<QuestionIdentifier>
					{q.id}
				</QuestionIdentifier>
				<SelectionIdentifier>query2_1</SelectionIdentifier>
			</Answer>
		</QuestionFormAnswers>

		Assert.assertEquals(Some(MultipleChoiceAnswer(mtc,
			mtc.options.map(o => o -> (o == "b")).toMap)), q.interpret(xml))
	}


	@Test
	def testInterpretationMultipleChoiceMultipleIDs: Unit = {
		val mtc = MultipleChoiceQuery("q", List("a", "b", "c"), 1)
		val q = MTQuery.convert(mtc)

		//Taken from http://docs.aws.amazon.com/AWSMechTurk/latest/AWSMturkAPI/ApiReference_QuestionFormAnswersDataStructureArticle.html
		val xml = <QuestionFormAnswers xmlns="[the QuestionFormAnswers schema URL]">
			<Answer>
				<QuestionIdentifier>asdf</QuestionIdentifier>
				<FreeText>C3</FreeText>
			</Answer>
			<Answer>
				<QuestionIdentifier>
					{q.id}
				</QuestionIdentifier>
				<SelectionIdentifier>query2_1</SelectionIdentifier>
			</Answer>
			<Answer>
				<QuestionIdentifier>
					ddd
				</QuestionIdentifier>
				<SelectionIdentifier>query2_1</SelectionIdentifier>
			</Answer>
		</QuestionFormAnswers>

		Assert.assertEquals(Some(MultipleChoiceAnswer(mtc,
			mtc.options.map(o => o -> (o == "b")).toMap)), q.interpret(xml))
	}


	@Test
	def testInterpretationMultipleChoiceWithNewLine: Unit = {
		val mtc = MultipleChoiceQuery("q", List("a\na", "b\nb", "c\nc"), 1)
		val q = MTQuery.convert(mtc)

		//Taken from http://docs.aws.amazon.com/AWSMechTurk/latest/AWSMturkAPI/ApiReference_QuestionFormAnswersDataStructureArticle.html
		val xml = <QuestionFormAnswers xmlns="[the QuestionFormAnswers schema URL]">
			<Answer>
				<QuestionIdentifier>asdf</QuestionIdentifier>
				<FreeText>C3</FreeText>
			</Answer>
			<Answer>
				<QuestionIdentifier>
					{q.id}
				</QuestionIdentifier>
				<SelectionIdentifier>query2_1</SelectionIdentifier>
			</Answer>
		</QuestionFormAnswers>

		Assert.assertEquals(Some(MultipleChoiceAnswer(mtc,
			mtc.options.map(o => o -> (o == "b\nb")).toMap)), q.interpret(xml))
	}
}
