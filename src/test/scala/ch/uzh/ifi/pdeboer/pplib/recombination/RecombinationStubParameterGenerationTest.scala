package ch.uzh.ifi.pdeboer.pplib.recombination

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 21/10/14.
 * TODO fix
 */
class RecombinationStubParameterGenerationTest {
	/*
	@Test
	def testParameterVariation(): Unit = {
		val defaultValueTestParam1: List[String] = List("a", "s")
		val defaultValueTestParam2: List[Integer] = List(1, 2, 3)
		val gen = new RecombinationStubParameterVariantGenerator(
			new TestRecombinationStub(Map.empty, List(
				new RecombinationParameter("testparam1", Some(defaultValueTestParam1)),
				new RecombinationParameter[Integer]("testparam2", Some(defaultValueTestParam2))
			))) {
			def paramVals = parameterValues
		}

		gen.initAllParamsWithCandidates()

		Assert.assertEquals(defaultValueTestParam1.toSet, gen.paramVals("testparam1").toSet.asInstanceOf[Set[String]])
		Assert.assertEquals(defaultValueTestParam2.toSet, gen.paramVals("testparam2").toSet.asInstanceOf[Set[String]])

		try {
			//TODO asInstanceOf required because result of implicit conv needs to be more specific than AnyRef. ugly :(
			gen.addParameterVariations("testparam1", List(1L).asInstanceOf[List[AnyRef]])
			Assert.assertFalse("type check failed", true)
		}
		catch {
			case i: IllegalArgumentException => Assert.assertTrue(true)
		}

		gen.addParameterVariations("testparam1", List("d"))
		Assert.assertEquals(("d" :: defaultValueTestParam1).toSet, gen.paramVals("testparam1").toSet.asInstanceOf[Set[String]])

		val variations = gen.generateParameterVariations()

		val expectedVariations = List(
			("a", defaultValueTestParam2),
			("s", defaultValueTestParam2),
			("d", defaultValueTestParam2)
		)

		Assert.assertTrue(expectedVariations.forall(v => v._2.forall(p => variations.exists(k => k("testparam1") == v._1 && k("testparam2") == p))))
		Assert.assertTrue(variations.size == 9)
	}

	private class TestRecombinationStub(params: Map[String, AnyRef], optionalParams: List[RecombinationParameter[_]]) extends RecombinationStub[String, String](params) {

		override def optionalParameters: List[RecombinationParameter[_]] = this.optionalParams

		override def run(data: String): String = data + "1"
	}
*/
}
