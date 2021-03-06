package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.examples.SurveyResult
import ch.uzh.ifi.pdeboer.pplib.hcomp._

/**
 * Created by pdeboer on 21/11/14.
 */
class MTSandboxTest {
	//@Test
	def testSendTextBox: Unit = {
		val r = HComp.mechanicalTurk.sendQueryAndAwaitResult(FreetextQuery("what's your name? <b>nothing much</b>"), HCompQueryProperties(5))
		val answer = r.get.is[FreetextAnswer]
		println(answer.answer)
	}

	//@Test
	def testExternal: Unit = {
		val q = ExternalQuery("https://lab.inventas-it.ch/externalsubmittest.php", "Patrick's great test", idFieldName = "testfield")
		val r = HComp.mechanicalTurk.sendQueryAndAwaitResult(q, HCompQueryProperties(5, qualifications = Nil))
		val answer = r.get.is[FreetextAnswer]
		println(answer.answer)
	}

	//@Test
	def testSendManyTextBoxQuestions: Unit = {
		import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
		(1 to 10).mpar.foreach(i => {
			val r = HComp.mechanicalTurk.sendQueryAndAwaitResult(FreetextQuery("what's your name? <b>nothing much</b>"), HCompQueryProperties(5))
			val answer = r.get.is[FreetextAnswer]
			println(answer.answer)
		})
	}

	//@Test
	def testSendSinglechoice: Unit = {
		val r = HComp.mechanicalTurk.sendQueryAndAwaitResult(MultipleChoiceQuery("what do you like best?", List("Tea", "Coffee", "Dr. Pepper", "Red Bull"), 1))
		val answer = r.get match {
			case a: MultipleChoiceAnswer => a.selectedAnswer
			case _ => println(r)
		}
		println(answer)
	}

	//@Test
	def testSendMultiplechoice: Unit = {
		val r = HComp.mechanicalTurk.sendQueryAndAwaitResult(MultipleChoiceQuery("what do you like best?", List("Tea", "Coffee", "Dr. Pepper", "Red Bull"), 10))
		val answer = r.get match {
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
			), "Please answer the following questions about you")).get.is[CompositeQueryAnswer]

		println(SurveyResult(
			name = result.get[FreetextAnswer](participantName).answer,
			continent = result.get[MultipleChoiceAnswer](participantContinent).selectedAnswer,
			age = result.get[FreetextAnswer](participantAge).answer.toInt
		))
	}
}
