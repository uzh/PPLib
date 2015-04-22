package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Collection
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 22/04/15.
 */
class RecombinationHintsTest {
	@Test
	def testAddHint: Unit = {
		val defaultHint: TrivialHint = new TrivialHint("default")
		val collectionHint = new TrivialHint("only with Collection")

		val hints = new RecombinationHints() += defaultHint
		val hints2 = hints.addHint(List(collectionHint), classOf[Collection])

		Assert.assertEquals(List(collectionHint), hints2(classOf[Collection]))
		Assert.assertEquals(List(defaultHint), hints2())
	}

	@Test
	def testBulkCreation: Unit = {
		val defaultHint: TrivialHint = new TrivialHint("default")
		val collectionHint = new TrivialHint("only with Collection")

		val hints = RecombinationHints.create(Map(
			RecombinationHints.DEFAULT_HINTS -> List(defaultHint),
			classOf[Collection] -> List(collectionHint)
		))

		Assert.assertEquals(List(collectionHint), hints(classOf[Collection]))
		Assert.assertEquals(List(defaultHint), hints())
	}


	private case class TrivialHint(name: String = "") extends RecombinationHint {
		override def filter[T <: ProcessStub[_, _]](clazz: Class[T]): Boolean = true

		override def processConstructionParameter: Map[String, Iterable[Any]] = Map.empty
	}

}
