package ch.uzh.ifi.pdeboer.crowdlang.recombination

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 20/10/14.
 */
class RecombinationStubParameterTest {
	@Test
	def testTypeSafetyOK(): Unit = {
		new TestRecombImplparams(chk = List(RecombinationParameter[String]("test")), Map("test" -> "asdf"))
		Assert.assertTrue(true) //no exception happened
	}

	@Test
	def testParameterDoesntExist(): Unit = {
		try {
			new TestRecombImplparams(chk = List(RecombinationParameter[String]("test2")), Map("test" -> "asdf"))
			Assert.assertFalse(true) //no exception here :(
		}
		catch {
			case e: IllegalArgumentException => Assert.assertTrue(true) //exception happened
		}
	}

	@Test
	def testTypeSafetyNotOK(): Unit = {
		try {
			new TestRecombImplparams(chk = List(RecombinationParameter[String]("test")), Map("test" -> List.empty[String]))
			Assert.assertTrue(false) //no exception happened. somethings wrong
		}
		catch {
			case e: IllegalArgumentException => Assert.assertTrue(true)
		}
	}

	private class TestRecombImplparams(chk: List[RecombinationParameter[_]], params: Map[String, AnyRef]) extends SimpleRecombinationStub[String, String](k => "asdf", params = params) {
		override def expectedParameters: List[RecombinationParameter[_]] = chk
	}

}
