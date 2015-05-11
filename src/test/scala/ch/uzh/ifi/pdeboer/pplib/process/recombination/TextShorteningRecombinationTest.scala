package ch.uzh.ifi.pdeboer.pplib.process.recombination

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
		val r = new Recombinator(RecombinationHints.create(RecombinatorTest.DEFAULT_TESTING_HINTS))
		r.materialize[CreateProcess[List[Patch], List[Patch]]]
	}
}
