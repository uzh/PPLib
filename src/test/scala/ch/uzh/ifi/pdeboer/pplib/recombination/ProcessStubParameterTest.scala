package ch.uzh.ifi.pdeboer.pplib.recombination

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 20/10/14.
 */
class ProcessStubParameterTest {
	@Test
	def testTypeSafetyOK(): Unit = {
		new TestRecombImplparams(chk = List(new ProcessParameter[String]("test", OtherParam())), Map(OtherParam() + "_test" -> "asdf"))
		Assert.assertTrue(true) //no exception happened
	}

	@Test
	def testParameterDoesntExist(): Unit = {
		try {
			new TestRecombImplparams(chk = List(new ProcessParameter[String]("test2", OtherParam())), Map(OtherParam() + "_test" -> "asdf"))
			Assert.assertFalse(true) //no exception here :(
		}
		catch {
			case e: Throwable => Assert.assertTrue(true) //exception happened
		}
	}

	@Test
	def testTypeSafetyNotOK(): Unit = {
		try {
			new TestRecombImplparams(chk = List(new ProcessParameter[String]("test", OtherParam())), Map(OtherParam() + "_test" -> List.empty[String]))
			Assert.assertTrue(false) //no exception happened. somethings wrong
		}
		catch {
			case e: AssertionError => Assert.assertTrue(true)
		}
	}

	private class TestRecombImplparams(chk: List[ProcessParameter[_]], params: Map[String, AnyRef]) extends ProcessStub[String, String](params = params) {
		override def expectedParametersOnConstruction: List[ProcessParameter[_]] = chk

		override def run(data: String): String = "test"
	}

}
