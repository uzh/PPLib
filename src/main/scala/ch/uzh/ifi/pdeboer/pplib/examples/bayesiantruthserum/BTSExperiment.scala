package ch.uzh.ifi.pdeboer.pplib.examples.bayesiantruthserum

import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{InstructionData, Patch, TrivialInstructionGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.BayesianTruthContest
import com.github.tototoshi.csv.{CSVReader, CSVWriter}
import scala.concurrent.duration._

/**
  * Created by pdeboer on 10/03/16.
  */
object BTSExperiment extends App {
	val decideInstructions = new TrivialInstructionGenerator("What is the capital of the state below?", "Please select the capital of this state", questionBetween = "Please select the city you think is the capital from the top of your head (no google) among the list below. ")

	val capitals = CSVReader.open("us capitals.csv")
	val csvWriter = CSVWriter.open("bts result.csv")

	capitals.all().slice(1, 2).foreach(state => {
		val stateName = state.head
		val capital = state(1)

		val cities = state.slice(1, 6)

		val contest = new BayesianTruthContest(Map(
			PORTAL_PARAMETER.key -> new MySQLDBPortalDecorator(HComp.mechanicalTurk),
			INSTRUCTIONS.key -> new InstructionData(actionName = "the same question. How likely is it that they give the answer below?", detailedDescription = "identify the capital of"),
			WORKER_COUNT.key -> 5,
			QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 18, cancelAndRepostAfter = 24 hours),
			INSTRUCTIONS_ITALIC.key -> stateName,
			OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(decideInstructions),
			MEMOIZER_NAME.key -> Some("btruth" + stateName)
		))

		val result = contest.process(cities.map(c => new Patch(c)))

		csvWriter.writeRow(List(stateName, capital, result))
	})

	csvWriter.close()
}