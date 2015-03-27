package ch.uzh.ifi.pdeboer.pplib.util

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 27/03/15.
 */
class ClassAncestorTest {
	@Test
	def trivial: Unit = {
		Assert.assertEquals(Set(classOf[A], classOf[B]), U.getAncestorsOfClass(classOf[C]))
	}

	private class A

	private class B extends A

	private class C extends B

}
