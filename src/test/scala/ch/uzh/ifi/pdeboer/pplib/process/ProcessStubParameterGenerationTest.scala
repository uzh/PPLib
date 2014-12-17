package ch.uzh.ifi.pdeboer.pplib.process

import ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{InstanciatedParameterVariantGenerator, TypedParameterVariantGenerator}
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 21/10/14.
 */
class ProcessStubParameterGenerationTest {

	import ch.uzh.ifi.pdeboer.pplib.process.ProcessStubParameterGenerationTest._
	import ch.uzh.ifi.pdeboer.pplib.process.TestProcessStub._

	@Test
	def testParameterVariation(): Unit = {
		val gen = new TypedParameterVariantGenerator[TestProcessStub]() {
			def paramVals = parameterValues
		}

		gen.initAllParamsWithCandidates()

		Assert.assertEquals(DEFAULT_VALUES_PARAM1.toSet, gen.paramVals(TEST_PARAM1.key).toSet.asInstanceOf[Set[String]])
		Assert.assertEquals(DEFAULT_VALUES_PARAM2.toSet, gen.paramVals(TEST_PARAM2.key).toSet.asInstanceOf[Set[String]])

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

		gen.addParameterVariations(TEST_PARAM1.key, List("d"))
		Assert.assertEquals(("d" :: DEFAULT_VALUES_PARAM1).toSet, gen.paramVals(TEST_PARAM1.key).toSet.asInstanceOf[Set[String]])

		val variations = gen.generateParameterVariations()

		val expectedVariations = List(
			("a", DEFAULT_VALUES_PARAM2),
			("s", DEFAULT_VALUES_PARAM2),
			("d", DEFAULT_VALUES_PARAM2)
		)

		Assert.assertTrue(expectedVariations.forall(v => v._2.forall(p => variations.exists(k => k(TEST_PARAM1.key) == v._1 && k(TEST_PARAM2.key) == p))))
		Assert.assertTrue(variations.size == 9)
	}


	@Test
	def testInstanciatedParamVariation(): Unit = {
		val gen = new InstanciatedParameterVariantGenerator(new TestProcessStub())
		val expectedParamVariations = gen.generateParameterVariations()
		Assert.assertTrue(expectedParamVariations.length > 0)

		val instanciatedVariants = gen.generateVariationsAndInstanciate()
		Assert.assertEquals(expectedParamVariations.length, instanciatedVariants.length)
		Assert.assertTrue("class correct", instanciatedVariants.forall(i => i.getClass == classOf[TestProcessStub]))
	}


	@Test
	def testPassableProcessParamVariation(): Unit = {
		//TODO code me
		val gen = new InstanciatedParameterVariantGenerator(new TestProcessStub())
		val expectedParamVariations = gen.generateParameterVariations()
		Assert.assertTrue(expectedParamVariations.length > 0)

		val passables = gen.generatePassableProcesses()
		Assert.assertEquals(expectedParamVariations.length, passables.length)
		Assert.assertTrue("class correct", passables.forall(i => i.getClass == classOf[PassableProcessParam[_, _]]))
	}

	@Test
	def testParameterVariationInstanciation(): Unit = {
		val gen = new TypedParameterVariantGenerator[TestProcessStub](initWithDefaults = true)
		val expectedParamVariations = gen.generateParameterVariations()
		Assert.assertTrue(expectedParamVariations.length > 0)

		val instanciatedVariants = gen.generateVariationsAndInstanciate()
		Assert.assertEquals(expectedParamVariations.length, instanciatedVariants.length)
		Assert.assertTrue("class correct", instanciatedVariants.forall(i => i.getClass == classOf[TestProcessStub]))
	}
}

object ProcessStubParameterGenerationTest {
	val DEFAULT_VALUES_PARAM1: List[String] = List("a", "s")
	val DEFAULT_VALUES_PARAM2: List[Integer] = List(1, 2, 3)
}

object TestProcessStub {
	val TEST_PARAM1 = new ProcessParameter[String]("testparam1", OtherParam(), Some(ProcessStubParameterGenerationTest.DEFAULT_VALUES_PARAM1))
	val TEST_PARAM2 = new ProcessParameter[Integer]("testparam2", OtherParam(), Some(ProcessStubParameterGenerationTest.DEFAULT_VALUES_PARAM2))
}

class TestProcessStub(params: Map[String, AnyRef] = Map.empty[String, AnyRef]) extends ProcessStub[String, String](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.TestProcessStub._

	override def optionalParameters: List[ProcessParameter[_]] = List(TEST_PARAM1, TEST_PARAM2)

	override def run(data: String): String = data + "1"
}