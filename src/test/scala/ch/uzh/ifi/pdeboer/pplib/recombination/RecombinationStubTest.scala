package ch.uzh.ifi.pdeboer.pplib.recombination

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 11/11/14.
 */
class RecombinationStubTest {
	@Test
	def testEqualsAndHashCode(): Unit = {
		Assert.assertEquals(new TestRecombinationStubA(), new TestRecombinationStubA())
		Assert.assertFalse(new TestRecombinationStubA().equals(new TestRecombinationStubAB()))
		Assert.assertFalse(new TestRecombinationStubA().equals(new TestRecombinationStubB()))
		Assert.assertFalse(new TestRecombinationStubA(Map("str" -> "a")).equals(new TestRecombinationStubA(Map("str" -> "b"))))
	}

	private class A(val a: String = "")

	private class TestRecombinationStubA(params: Map[String, Any] = Map.empty[String, Any]) extends RecombinationStub[String, A](params) {
		override protected def run(data: String): A = {
			new A(data)
		}
	}

	private class TestRecombinationStubAB(params: Map[String, Any] = Map.empty[String, Any]) extends TestRecombinationStubA(params) {
		override protected def run(data: String): A = {
			new A(data)
		}
	}

	private class TestRecombinationStubB(params: Map[String, Any] = Map.empty[String, Any]) extends RecombinationStub[String, A](params) {
		override protected def run(data: String): A = {
			new A(data + "B")
		}
	}

}
