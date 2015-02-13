package ch.uzh.ifi.pdeboer.pplib.process

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 11/11/14.
 */
class ProcessStubTest {
	@Test
	def testEqualsAndHashCode(): Unit = {
		Assert.assertEquals(new TestProcessStubA(), new TestProcessStubA())
		Assert.assertFalse(new TestProcessStubA().equals(new TestProcessStubAB()))
		Assert.assertFalse(new TestProcessStubA().equals(new TestProcessStubB()))
		Assert.assertFalse(new TestProcessStubA(Map("str" -> "a")).equals(new TestProcessStubA(Map("str" -> "b"))))
	}

	private class A(val a: String = "")

	private class TestProcessStubA(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, A](params) {
		override protected def run(data: String): A = {
			new A(data)
		}
	}

	private class TestProcessStubAB(params: Map[String, Any] = Map.empty[String, Any]) extends TestProcessStubA(params) {
		override protected def run(data: String): A = {
			new A(data)
		}
	}

	private class TestProcessStubB(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, A](params) {
		override protected def run(data: String): A = {
			new A(data + "B")
		}
	}

}
