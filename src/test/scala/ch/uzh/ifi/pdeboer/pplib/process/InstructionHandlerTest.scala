package ch.uzh.ifi.pdeboer.pplib.process

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTupleStringified, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Collection
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 20/02/15.
 */
class InstructionHandlerTest {
	@Test
	def testOverridingInstructionHandler(): Unit = {
		val expectedInstructions: HCompInstructionsWithTupleStringified = HCompInstructionsWithTupleStringified("blupp")
		val collector = new PassableProcessParam[Patch, List[Patch]](classOf[Collection], Map(
			WORKER_COUNT.key -> 1,
			QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 20),
			OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(new ExplicitInstructionGenerator(expectedInstructions, "blupp2")),
			MEMOIZER_NAME.key -> Some("implementedBefore")
		))
		val coll = collector.create().asInstanceOf[ProcessStub[Patch, List[Patch]] with InstructionHandler]
		Assert.assertEquals(expectedInstructions, coll.instructions)
	}
}
