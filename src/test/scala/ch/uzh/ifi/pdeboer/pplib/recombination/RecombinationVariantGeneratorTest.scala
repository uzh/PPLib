package ch.uzh.ifi.pdeboer.pplib.recombination

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 10/10/14.
 */
class RecombinationVariantGeneratorTest {

	@Test
	def testSimpleCombination(): Unit = {
		val list1 = List(new TestRecombinationStub(1), new TestRecombinationStub(2))
		val list2 = List(new TestRecombinationStub(3))
		val list3 = List(new TestRecombinationStub(4), new TestRecombinationStub(5))

		val configMap = List(("list1", list1), ("list2", list2), ("list3", list3)).toMap

		val gen = new RecombinationVariantGenerator(configMap)
		Assert.assertTrue(gen.variants.size > 0)

		val expectedCombinations = List(
			List(1, 3, 4), List(1, 3, 5),
			List(2, 3, 4), List(2, 3, 5)
		)

		val actualCombinations = gen.variants.map(r => {
			val stub = r.config.asInstanceOf[Map[String, TestRecombinationStub]]
			List(stub("list1").id, stub("list2").id, stub("list3").id)
		}).toList

		Assert.assertTrue(listContentsEqual(expectedCombinations, actualCombinations))
	}

	private def listContentsEqual(a: List[_], b: List[_]) = {
		a.forall(b.contains(_)) && b.forall(a.contains(_))
	}

	private class TestRecombinationStub(val id: Int) extends RecombinationStub[Integer, Integer] {
		override def run[I >: Integer, O >: Integer](data: I): O = data.asInstanceOf[Integer]
	}

}