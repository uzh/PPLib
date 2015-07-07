package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.util.{ProcessPrinter, TestUtils}
import org.junit.{Assert, Test}

import scala.reflect.io.File

/**
 * Created by pdeboer on 04/05/15.
 */
class TextShorteningRecombinationTest {
	@Test
	def generateTextShorteningRecombinations: Unit = {
		TestUtils.ensureThereIsAtLeast1Portal()

		val toStore = <Data>
			{candidates.map(c => {
				new ProcessPrinter(c, Some(Nil)).lines
			})}
		</Data>

		File("test.xml").writeAll(toStore + "")

		Assert.assertEquals("We should have 19 recombinations", 19, candidates.size)
	}

	lazy val candidates = {
		val r = new TypeRecombinator(RecombinationHints.create(TypeRecombinatorTest.DEFAULT_TESTING_HINTS))
		r.materialize[CreateProcess[_ <: List[Patch], _ <: List[Patch]]]
	}
}