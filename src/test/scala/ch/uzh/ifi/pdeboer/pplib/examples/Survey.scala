package ch.uzh.ifi.pdeboer.pplib.examples

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.process.ProcessStubWithHCompPortalAccess
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.SimpleWriteProcess

/**
 * Created by pdeboer on 30/10/14.
 */
object Survey extends App {
	private val participantCount: Int = 2

	val submissions = (1 to participantCount).par.map(n => {
		val participantName = FreetextQuery("What's your name?")
		val participantContinent = MultipleChoiceQuery("Where are you from?",
			List("Europe", "Americas", "Asia", "Australia", "Africa"),
			maxNumberOfResults = 1)
		val participantAge = FreetextQuery("How old are you?")

		val result = HComp("crowdFlower").sendQueryAndAwaitResult(
			CompositeQuery(List(
				participantName,
				participantContinent,
				participantAge
			), "Please answer the following questions about you")).get.as[CompositeQueryAnswer]

		SurveyResult(
			name = result.get[FreetextAnswer](participantName).answer,
			continent = result.get[MultipleChoiceAnswer](participantContinent).selectedAnswer,
			age = result.get[FreetextAnswer](participantAge).answer.toInt
		)
	})

	println(s"The max age of the participants is ${submissions.maxBy(_.age).age}")
	println(s"Names of people participated are ${submissions.map(_.name).mkString(", ")}")
	println(s"Here's a list of people per country ${
		submissions.groupBy(_.continent).map {
			case (continent, participants) => s"$continent:${participants.length}"
		}.mkString(", ")
	}")
}

case class SurveyResult(name: String, continent: String, age: Int) {
	println(s" created $this")
}