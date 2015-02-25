package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.process.entities.{Patch, PassableProcessParam}
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
		Assert.assertTrue(c.createdProcesses(0).asInstanceOf[CreateSignalingProcess[_, _]].called)
		Assert.assertTrue(d.createdProcesses(0).asInstanceOf[CreateSignalingProcess[_, _]].called)
	}

	@Test
	def testDefaultParam: Unit = {
		val (c, d) = (collectProcess, decideProcess)
		new CollectDecideProcess(Map(COLLECT.key -> c, DECIDE.key -> d)).process(new Patch("test"))
		Assert.assertTrue(c.createdProcesses(0).asInstanceOf[CreateSignalingProcess[_, _]].called)
		Assert.assertTrue(d.createdProcesses(0).asInstanceOf[CreateSignalingProcess[_, _]].called)
	}

	def collectProcess = new PassableProcessParam[CreateSignalingProcess[Patch, List[Patch]]](Map(CreateSignalingProcess.OUTPUT.key -> List("a", "b").map(l => new Patch(l))), Some(new CreateSignalingProcessFactory()))

	def decideProcess = new PassableProcessParam[CreateSignalingProcess[List[Patch], Patch]](Map(CreateSignalingProcess.OUTPUT.key -> new Patch("a")), Some(new CreateSignalingProcessFactory()))
}
