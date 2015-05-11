package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.util.RecursiveProcessPrinter
import org.junit.{Assert, Test}

import scala.reflect.io.File

/**
 * Created by pdeboer on 04/05/15.
 */
class TextShorteningRecombinationTest {
	@Test
	def generateTextShorteningRecombinations: Unit = {
		val toStore = <Data>
			{candidates.map(c => {
				new RecursiveProcessPrinter(c, Some(Nil)).lines
			})}
		</Data>

		File("test.xml").writeAll(toStore + "")


		Assert.assertEquals("We should have 9 recombinations", 9, candidates.size)
	}

	lazy val candidates: List[PassableProcessParam[CreateProcess[List[Patch], List[Patch]]]] = {
		val r = new Recombinator(RecombinationHints.create(Map(
			RecombinationHints.DEFAULT_HINTS -> List(
				//disable using all portals as targets. only use MTurk
				new SettingsOnParamsRecombinationHint(List(DefaultParameters.PORTAL_PARAMETER.key), addDefaultValuesForParam = Some(false)),
				new AddedParameterRecombinationHint[HCompPortalAdapter](DefaultParameters.PORTAL_PARAMETER, List(HComp.mechanicalTurk)),

				//disable default values for instruction values
				new SettingsOnParamsRecombinationHint(List(DefaultParameters.INSTRUCTIONS.key), addDefaultValuesForParam = Some(false)),
				new AddedParameterRecombinationHint[InstructionData](DefaultParameters.INSTRUCTIONS, List(
					new InstructionData(actionName = "shorten the following paragraph", detailedDescription = "grammar (e.g. tenses), text-length")))
			)
		)))
		r.materialize[CreateProcess[List[Patch], List[Patch]]]
	}
}
