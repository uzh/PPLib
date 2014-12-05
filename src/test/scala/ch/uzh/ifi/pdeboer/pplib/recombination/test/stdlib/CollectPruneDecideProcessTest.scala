package ch.uzh.ifi.pdeboer.pplib.recombination.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.CollectPruneDecideProcess
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 05/12/14.
 */
class CollectPruneDecideProcessTest {

	import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.CollectPruneDecideProcess._

	@Test
	def testEachProcessIsCalled: Unit = {
		val (c, p, d) = (collectProcess, pruneProcess, decideProcess)
		new CollectPruneDecideProcess(Map(COLLECT.key -> c, PRUNE.key -> p, DECIDE.key -> d)).process("test")
		Assert.assertTrue(c.called)
		Assert.assertTrue(p.called)
		Assert.assertTrue(d.called)
	}

	@Test
	def testDefaultParam: Unit = {
		val (c, d) = (collectProcess, decideProcess)
		new CollectPruneDecideProcess(Map(COLLECT.key -> c, DECIDE.key -> d)).process("test")
		Assert.assertTrue(c.called)
		Assert.assertTrue(d.called)
	}

	def collectProcess = new SignalingProcess[String, List[String]](List("a", "b"))

	def pruneProcess = new SignalingProcess[List[String], List[String]](List("b"))

	def decideProcess = new SignalingProcess[List[String], String]("b")

}
