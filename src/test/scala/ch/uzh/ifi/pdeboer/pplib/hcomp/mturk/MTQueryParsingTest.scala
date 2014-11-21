package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextAnswer, FreetextQuery, MultipleChoiceAnswer, MultipleChoiceQuery}
import ch.uzh.ifi.pdeboer.pplib.util.U._
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 20/11/14.
 */
class MTQueryParsingTest {
	@Test
	def testFreetextQueryXML(): Unit = {
		val q = FreetextQuery("question", defaultAnswer = "default", title = "title")
		val mtQ = MTQuery.convert(q).asInstanceOf[MTFreeTextQuery]
		Assert.assertEquals(
			removeWhitespaces(<FreeTextAnswer>
				<DefaultText>default</DefaultText>
			</FreeTextAnswer>.toString)
			, removeWhitespaces(mtQ.elementXML + ""))
	}

	@Test
	def testRemoveWhitespaces: Unit = {
		Assert.assertEquals("addfs", removeWhitespaces("a d	d\nf s"))
	}


	@Test
	def testFreetextQueryXMLNoDefault(): Unit = {
		val q = FreetextQuery("question")
		val mtQ = MTQuery.convert(q).asInstanceOf[MTFreeTextQuery]
		Assert.assertEquals(
			removeWhitespaces(<FreeTextAnswer></FreeTextAnswer>.toString())
			, removeWhitespaces(mtQ.elementXML.toString()))
	}

	@Test
	def testMultipleChoice: Unit = {
		val q = MultipleChoiceQuery("qestion", List("a", "b", "c"), 2, 1)
		val mtQ = MTQuery.convert(q).asInstanceOf[MTMultipleChoiceQuery]
		Assert.assertEquals(
			removeWhitespaces(<SelectionAnswer>
				<MinSelectionCount>1</MinSelectionCount>
				<MaxSelectionCount>2</MaxSelectionCount>
			</SelectionAnswer>.toString()), removeWhitespaces(mtQ.elementXML.toString()))
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

		Assert.assertEquals(FreetextAnswer(ftq, "C3"), q.interpret(xml))
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
				<SelectionIdentifier>b</SelectionIdentifier>
			</Answer>
		</QuestionFormAnswers>

		Assert.assertEquals(MultipleChoiceAnswer(mtc,
			mtc.options.map(o => o -> (o == "b")).toMap), q.interpret(xml))
	}
}
