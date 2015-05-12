package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, PassableProcessParam}
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

		val findProc = new PassableProcessParam[DecideSignalingProcess[List[IndexedPatch], List[IndexedPatch]]](Map(DecideSignalingProcess.OUTPUT.key -> dataReturnedByFinder), Some(new DecideSignalingProcessFactory()))
		val fixProc = new PassableProcessParam[FixSignalingProcess](Map(FixSignalingProcess.OUTPUT.key -> dataReturnedByFixer), Some(new FixSignalingProcessFactory()))
		val ffp = new FindFixPatchProcess(Map(FindFixPatchProcess.FIND_PROCESS.key -> findProc, FindFixPatchProcess.FIX_PROCESS.key -> fixProc))

		Assert.assertEquals(dataReturnedByFixer ::: allData.takeRight(2), ffp.process(allData))
		Assert.assertEquals(1, findProc.createdProcesses.size)
		Assert.assertEquals(1, fixProc.createdProcesses.size)
		Assert.assertTrue(findProc.createdProcesses.head.asInstanceOf[DecideSignalingProcess[_, _]].called)
		Assert.assertTrue(fixProc.createdProcesses.head.called)
	}
}
