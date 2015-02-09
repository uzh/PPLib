package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.parameter.{IndexedPatch, PassableProcessParam}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FindFixPatchProcess
import junit.framework.Assert
import org.junit.Test

/**
 * Created by pdeboer on 23/12/14.
 */
class FindFixPatchProcessTest {
	@Test
	def testPatchTogether: Unit = {
		val allData = List("a", "b", "c", "d", "e").zipWithIndex.map(i => new IndexedPatch(i))
		val dataReturnedByFinder = allData.take(3)
		val dataReturnedByFixer = dataReturnedByFinder.map(d => d.duplicate(d.value + "1"))

		val findProc = new PassableProcessParam[List[IndexedPatch], List[IndexedPatch]](classOf[SignalingProcess[List[IndexedPatch], List[IndexedPatch]]], Map(SignalingProcess.OUTPUT.key -> dataReturnedByFinder), Some(new SignalingProcessFactory()))
		val fixProc = new PassableProcessParam[List[IndexedPatch], List[IndexedPatch]](classOf[SignalingProcess[List[IndexedPatch], List[IndexedPatch]]], Map(SignalingProcess.OUTPUT.key -> dataReturnedByFixer), Some(new SignalingProcessFactory()))
		val ffp = new FindFixPatchProcess(Map(FindFixPatchProcess.FIND_PROCESS.key -> findProc, FindFixPatchProcess.FIX_PROCESS.key -> fixProc))

		Assert.assertEquals(dataReturnedByFixer ::: allData.takeRight(2), ffp.process(allData))
		Assert.assertEquals(1, findProc.createdProcesses.size)
		Assert.assertEquals(1, fixProc.createdProcesses.size)
		Assert.assertTrue(findProc.createdProcesses(0).asInstanceOf[SignalingProcess[_, _]].called)
		Assert.assertTrue(fixProc.createdProcesses(0).asInstanceOf[SignalingProcess[_, _]].called)
	}
}
