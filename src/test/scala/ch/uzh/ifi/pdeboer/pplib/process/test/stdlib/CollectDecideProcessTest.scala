package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.process.parameter.{Patch, PassableProcessParam}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectDecideProcess
import org.junit.{Before, Assert, Test}

/**
 * Created by pdeboer on 05/12/14.
 */
class CollectDecideProcessTest {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectDecideProcess._

	@Before
	def rmState(): Unit = {
		new File("state/").delete()
	}


	@Test
	def testEachProcessIsCalled: Unit = {
		val (c, d) = (collectProcess, decideProcess)
		new CollectDecideProcess(Map(COLLECT.key -> c, DECIDE.key -> d)).process(new Patch("test"))
		Assert.assertTrue(c.createdProcesses(0).asInstanceOf[SignalingProcess[_, _]].called)
		Assert.assertTrue(d.createdProcesses(0).asInstanceOf[SignalingProcess[_, _]].called)
	}

	@Test
	def testDefaultParam: Unit = {
		val (c, d) = (collectProcess, decideProcess)
		new CollectDecideProcess(Map(COLLECT.key -> c, DECIDE.key -> d)).process(new Patch("test"))
		Assert.assertTrue(c.createdProcesses(0).asInstanceOf[SignalingProcess[_, _]].called)
		Assert.assertTrue(d.createdProcesses(0).asInstanceOf[SignalingProcess[_, _]].called)
	}

	def collectProcess = new PassableProcessParam[Patch, List[Patch]](classOf[SignalingProcess[Patch, List[Patch]]], Map(SignalingProcess.OUTPUT.key -> List("a", "b").map(l => new Patch(l))), Some(new SignalingProcessFactory()))

	def decideProcess = new PassableProcessParam[List[Patch], Patch](classOf[SignalingProcess[List[Patch], Patch]], Map(SignalingProcess.OUTPUT.key -> new Patch("a")), Some(new SignalingProcessFactory()))
}
