package ch.uzh.ifi.pdeboer.pplib.recombination

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 21/10/14.
 */
class RecombinationStubParameterGenerationTest {

	import RecombinationStubParameterGenerationTest._
	@Test
	def testParameterVariation(): Unit = {
		val gen = new RecombinationStubParameterVariantGenerator[TestRecombinationStub]() {
			def paramVals = parameterValues
		}

		gen.initAllParamsWithCandidates()

		Assert.assertEquals(DEFAULT_VALUES_PARAM1.toSet, gen.paramVals("testparam1").toSet.asInstanceOf[Set[String]])
		Assert.assertEquals(DEFAULT_VALUES_PARAM2.toSet, gen.paramVals("testparam2").toSet.asInstanceOf[Set[String]])

		/*
		//TODO type checking functionality doesnt work
		try {
			//asInstanceOf required because result of implicit conv needs to be more specific than AnyRef. ugly :(
			gen.addParameterVariations("testparam1", List(1L).asInstanceOf[List[AnyRef]])
			Assert.assertFalse("type check failed", true)
		}
		catch {
			case i: IllegalArgumentException => Assert.assertTrue(true)
		}
		*/

		gen.addParameterVariations("testparam1", List("d"))
		Assert.assertEquals(("d" :: DEFAULT_VALUES_PARAM1).toSet, gen.paramVals("testparam1").toSet.asInstanceOf[Set[String]])

		val variations = gen.generateParameterVariations()

		val expectedVariations = List(
			("a", DEFAULT_VALUES_PARAM2),
			("s", DEFAULT_VALUES_PARAM2),
			("d", DEFAULT_VALUES_PARAM2)
		)

		Assert.assertTrue(expectedVariations.forall(v => v._2.forall(p => variations.exists(k => k("testparam1") == v._1 && k("testparam2") == p))))
		Assert.assertTrue(variations.size == 9)
	}

	@Test
	def testParameterVariationInstanciation(): Unit = {
		val gen = new RecombinationStubParameterVariantGenerator[TestRecombinationStub](initWithDefaults = true)
		val expectedParamVariations = gen.generateParameterVariations()
		Assert.assertTrue(expectedParamVariations.length > 0)

		val instanciatedVariants = gen.generateVariationsAndInstanciate()
		Assert.assertEquals(expectedParamVariations.length, instanciatedVariants.length)
		Assert.assertTrue("class correct", instanciatedVariants.forall(i => i.getClass == classOf[TestRecombinationStub]))
	}
}

object RecombinationStubParameterGenerationTest {
	val DEFAULT_VALUES_PARAM1: List[String] = List("a", "s")
	val DEFAULT_VALUES_PARAM2: List[Integer] = List(1, 2, 3)
}

class TestRecombinationStub(params: Map[String, AnyRef]) extends RecombinationStub[String, String](params) {
	override def optionalParameters: List[RecombinationParameter[_]] = List(
		new RecombinationParameter[String]("testparam1", Some(RecombinationStubParameterGenerationTest.DEFAULT_VALUES_PARAM1)),
		new RecombinationParameter[Integer]("testparam2", Some(RecombinationStubParameterGenerationTest.DEFAULT_VALUES_PARAM2))
	)

	override def run(data: String): String = data + "1"
}