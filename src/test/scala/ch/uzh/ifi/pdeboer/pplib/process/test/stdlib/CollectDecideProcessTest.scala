package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectDecideProcess
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 05/12/14.
 */
class CollectDecideProcessTest {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectDecideProcess._

	@Test
	def testEachProcessIsCalled: Unit = {
		val (c, d) = (collectProcess, decideProcess)
		new CollectDecideProcess(Map(COLLECT.key -> c, DECIDE.key -> d)).process("test")
		Assert.assertTrue(c.createdProcesses(0).asInstanceOf[SignalingProcess[_, _]].called)
		Assert.assertTrue(d.createdProcesses(0).asInstanceOf[SignalingProcess[_, _]].called)
	}

	@Test
	def testDefaultParam: Unit = {
		val (c, d) = (collectProcess, decideProcess)
		new CollectDecideProcess(Map(COLLECT.key -> c, DECIDE.key -> d)).process("test")
		Assert.assertTrue(c.createdProcesses(0).asInstanceOf[SignalingProcess[_, _]].called)
		Assert.assertTrue(d.createdProcesses(0).asInstanceOf[SignalingProcess[_, _]].called)
	}

	def collectProcess = new PassableProcessParam[String, List[String]](classOf[SignalingProcess[String, List[String]]], Map(SignalingProcess.OUTPUT.key -> List("a", "b")), Some(new SignalingProcessFactory()))

	def decideProcess = new PassableProcessParam[List[String], String](classOf[SignalingProcess[List[String], String]], Map(SignalingProcess.OUTPUT.key -> "a"), Some(new SignalingProcessFactory()))

}
