package ch.uzh.ifi.pdeboer.pplib.process

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 20/10/14.
 */
class ProcessStubParameterTest {
	@Test
	def testTypeSafetyOK(): Unit = {
		new TestRecombImplparams(paramsOnConstruct = List(new ProcessParameter[String]("test", OtherParam())), params = Map(OtherParam() + "_test" -> "asdf"))
		Assert.assertTrue(true) //no exception happened
	}

	@Test
	def testParameterDoesntExist(): Unit = {
		try {
			new TestRecombImplparams(paramsOnConstruct = List(new ProcessParameter[String]("test2", OtherParam())), params = Map(OtherParam() + "_test" -> "asdf"))
			Assert.assertFalse(true) //no exception here :(
		}
		catch {
			case e: Throwable => Assert.assertTrue(true) //exception happened
		}
	}

	@Test
	def testExpectedParamExists: Unit = {
		val params = new ProcessParameter[String]("test222", OtherParam(), None)
		val proc = new TestRecombImplparams(paramsOnRun = List(params))
		try {
			proc.process("bla")
			Assert.assertTrue(false) //should throw exception
		}
		catch {
			case e: Exception => Assert.assertTrue(true)
		}
	}

	@Test
	def testExpectedParamExistsAndItDoes: Unit = {
		val params = new ProcessParameter[String]("test", OtherParam(), None)
		val proc = new TestRecombImplparams(paramsOnRun = List(params), params = Map(params.key -> "blupp"))
		proc.process("bla")
		Assert.assertTrue(true) //should throw exception
		Assert.assertEquals("blupp", proc.getParam(params))
	}

	@Test
	def testTypeSafetyNotOK(): Unit = {
		try {
			new TestRecombImplparams(paramsOnConstruct = List(new ProcessParameter[String]("test", OtherParam())), params = Map(OtherParam() + "_test" -> List.empty[String]))
			Assert.assertTrue(false) //no exception happened. somethings wrong
		}
		catch {
			case e: AssertionError => Assert.assertTrue(true)
		}
	}

	private class TestRecombImplparams(paramsOnConstruct: List[ProcessParameter[_]] = Nil, paramsOnRun: List[ProcessParameter[_]] = Nil, params: Map[String, AnyRef] = Map.empty) extends ProcessStub[String, String](params = params) {
		override def expectedParametersOnConstruction: List[ProcessParameter[_]] = paramsOnConstruct


		override def expectedParametersBeforeRun: List[ProcessParameter[_]] = paramsOnRun

		override def run(data: String): String = "test"
	}

}
