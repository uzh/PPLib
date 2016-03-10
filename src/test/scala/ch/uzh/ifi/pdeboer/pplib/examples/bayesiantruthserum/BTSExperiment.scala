package ch.uzh.ifi.pdeboer.pplib.examples.bayesiantruthserum

import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, InstructionData, TrivialInstructionGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.BayesianTruthContest

/**
  * Created by pdeboer on 10/03/16.
  */
object BTSExperiment extends App {
	val decideInstructions = new TrivialInstructionGenerator("What is the capital of the state below?", "Please select the capital of this state", questionBetween = "Please select the city you think is the capital from the top of your head (no google) among the list below. ")

	val contest = new BayesianTruthContest(Map(
		PORTAL_PARAMETER.key -> new MySQLDBPortalDecorator(HComp.mechanicalTurk),
		INSTRUCTIONS.key -> new InstructionData(actionName = "the same question. How likely is it that they give the answer below?", detailedDescription = "identify the capital of"),
		WORKER_COUNT.key -> 5,
		QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 18, qualifications = Nil),
		INSTRUCTIONS_ITALIC.key -> "New York",
		OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(decideInstructions)
	))

	val result = contest.process(IndexedPatch.from("New York,Albany,Manhattan,Brooklyn", ","))
	println(s"the answer was $result")
}