package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectPruneDecideProcess._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.SelectPruneProcess
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 05/12/14.
 */
class SelectPruneProcessTest {
	@Test
	def testEachProcessIsCalled: Unit = {
		import ch.uzh.ifi.pdeboer.pplib.process.stdlib.SelectPruneProcess._
		val (c, p) = (collectProcess, pruneProcess)
		new SelectPruneProcess(Map(SELECT.key -> c, PRUNE.key -> p)).process(List("asdf"))
		Assert.assertTrue(c.called)
		Assert.assertTrue(p.called)
	}

	def collectProcess = new SignalingProcess[List[String], List[String]](List("a", "b"))

	def pruneProcess = new SignalingProcess[List[String], List[String]](List("b"))
}
