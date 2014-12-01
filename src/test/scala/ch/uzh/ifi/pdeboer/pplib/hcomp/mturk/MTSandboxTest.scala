package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.examples.SurveyResult
import ch.uzh.ifi.pdeboer.pplib.hcomp._
import org.junit.Test

/**
 * Created by pdeboer on 21/11/14.
 */
class MTSandboxTest {
	//@Test
	def testSendTextBox: Unit = {
		val r = HComp.mechanicalTurk.sendQueryAndAwaitResult(FreetextQuery("what's your name? <b>nothing much</b>"), HCompQueryProperties(5))
		val answer = r.get.as[FreetextAnswer]
		println(answer.answer)
	}

	//@Test
	def testSendSinglechoice: Unit = {
		val r = HComp.mechanicalTurk.sendQueryAndAwaitResult(MultipleChoiceQuery("what do you like best?", List("Tea", "Coffee", "Dr. Pepper", "Red Bull"), 1))
		val answer = r match {
			case a: MultipleChoiceAnswer => a.selectedAnswer
			case _ => println(r)
		}
		println(answer)
	}

	//@Test
	def testSendMultiplechoice: Unit = {
		val r = HComp.mechanicalTurk.sendQueryAndAwaitResult(MultipleChoiceQuery("what do you like best?", List("Tea", "Coffee", "Dr. Pepper", "Red Bull"), 10))
		val answer = r match {
			case a: MultipleChoiceAnswer => a.selectedAnswer
			case _ => println(r)
		}
		println(answer)
	}

	//@Test
	def testComposite: Unit = {
		val participantName = FreetextQuery("What's your name?")
		val participantContinent = MultipleChoiceQuery("Where are you from?",
			List("Europe", "Americas", "Asia", "Australia", "Africa"),
			maxNumberOfResults = 1)
		val participantAge = FreetextQuery("How old are you?")

		val result = HComp(MechanicalTurkPortalAdapter.PORTAL_KEY).sendQueryAndAwaitResult(
			CompositeQuery(List(
				participantName,
				participantContinent,
				participantAge
			), "Please answer the following questions about you")).get.as[CompositeQueryAnswer]

		println(SurveyResult(
			name = result.get[FreetextAnswer](participantName).answer,
			continent = result.get[MultipleChoiceAnswer](participantContinent).selectedAnswer,
			age = result.get[FreetextAnswer](participantAge).answer.toInt
		))
	}
}
