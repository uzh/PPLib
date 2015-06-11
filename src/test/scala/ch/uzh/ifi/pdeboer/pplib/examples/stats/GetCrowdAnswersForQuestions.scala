package ch.uzh.ifi.pdeboer.pplib.examples.stats

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Collection

/**
 * Created by pdeboer on 11/06/15.
 */
class GetCrowdAnswersForQuestions(data: List[QuestionData]) {
	val process = new Collection(Map(
		DefaultParameters.PORTAL_PARAMETER.key -> HComp.mechanicalTurk,
		DefaultParameters.WORKER_COUNT.key -> 10,
		DefaultParameters.QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 16)
		//DefaultParameters.OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(new ExplicitInstructionGenerator() )
	))


}
