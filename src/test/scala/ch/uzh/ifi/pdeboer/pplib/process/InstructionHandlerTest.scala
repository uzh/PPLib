package ch.uzh.ifi.pdeboer.pplib.process

import ch.uzh.ifi.pdeboer.pplib.hcomp.{StringQuestionRenderer, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Collection
import org.junit.{Assert, Test}

import scala.reflect.runtime.universe._
/**
 * Created by pdeboer on 20/02/15.
 */
class InstructionHandlerTest {
	@Test
	def testOverridingInstructionHandler(): Unit = {
		val expectedInstructions: StringQuestionRenderer = StringQuestionRenderer("blupp")
		val collector = new PassableProcessParam[Collection](Map(
			WORKER_COUNT.key -> 1,
			QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 20),
			OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(new ExplicitInstructionGenerator(expectedInstructions, "blupp2")),
			MEMOIZER_NAME.key -> Some("implementedBefore")
		))
		val coll = collector.create()
		Assert.assertEquals(expectedInstructions, coll.instructions)
	}

	@Test
	def testInstructionGeneratorPool(): Unit = {
		val theGenerator = new TrivialInstructionGenerator("before", "title")
		val expectedInstructions = theGenerator.generateQuestion(null)

		val collector = new PassableProcessParam[Collection](Map(
			WORKER_COUNT.key -> 1,
			QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 20),
			DefaultParameters.INSTRUCTION_GENERATOR_POOL.key -> Map(typeOf[CreateProcess[_, _]] -> theGenerator)
		))
		val coll = collector.create()
		Assert.assertEquals(expectedInstructions, coll.instructions)
	}

	@Test
	def testInstructionGeneratorPoolAndOverridenHandler(): Unit = {
		val theGenerator = new TrivialInstructionGenerator("before", "title")
		val expectedInstructions = StringQuestionRenderer("blupp")

		val collector = new PassableProcessParam[Collection](Map(
			WORKER_COUNT.key -> 1,
			QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 20),
			DefaultParameters.INSTRUCTION_GENERATOR_POOL.key -> Map(typeOf[CreateProcess[_, _]] -> List(theGenerator)),
			OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(new ExplicitInstructionGenerator(expectedInstructions, "blupp2"))
		))
		val coll = collector.create()
		Assert.assertEquals(expectedInstructions, coll.instructions)
	}
}
