package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 04/05/15.
 */
class TextShorteningRecombinationTest {
	@Test
	def generateTextShorteningRecombinations: Unit = {
		Assert.assertEquals("We should have 63 recombinations", 63, candidates.size)
	}

	lazy val candidates: List[PassableProcessParam[CreateProcess[Patch, Patch]]] = {
		val r = new Recombinator(RecombinationHints.create(Map(
			RecombinationHints.DEFAULT_HINTS -> List(
				//disable using all portals as targets. only use MTurk
				new SettingsOnParamsRecombinationHint(List(DefaultParameters.PORTAL_PARAMETER.key), addDefaultValuesForParam = Some(false)),
				new AddedParameterRecombinationHint[HCompPortalAdapter](DefaultParameters.PORTAL_PARAMETER, List(HComp.mechanicalTurk)),

				//other params
				new AddedParameterRecombinationHint[Int](DefaultParameters.WORKER_COUNT, List(3)),
				new AddedParameterRecombinationHint[InstructionData](DefaultParameters.INSTRUCTIONS, List(
					new InstructionData(actionName = "shorten the following paragraph", detailedDescription = "grammar (e.g. tenses), text-length")))
			)
		)))
		r.materialize[CreateProcess[Patch, Patch]]
	}
}
