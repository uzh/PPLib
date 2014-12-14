package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.process.{NoProcessMemoizer, ProcessMemoizer}
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

/**
 * Created by pdeboer on 14/12/14.
 */
class FixPatchExecuter(@transient val driver: FixPatchDriver,
					   val allOrderedPatches: List[Patch],
					   val indicesOfPatchesToFix: List[Int],
					   val patchesToIncludeBeforeAndAfterMain: (Int, Int) = (2, 2),
					   @transient val memoizer: ProcessMemoizer = new NoProcessMemoizer()) {

	lazy val allFixedPatches: List[(Int, Patch)] = {
		indicesOfPatchesToFix.mpar.map(i => (i, getFixForPatchAtIndex(i))).toList
	}

	lazy val allPatches: List[Patch] = allOrderedPatches.zipWithIndex.map(p => {
		val possibleFix: Option[Patch] = allFixedPatches.find(_._1 == p._2).map(_._2)
		possibleFix.getOrElse(p._1)
	})

	def getFixForPatchAtIndex(index: Int) = driver.fix(allOrderedPatches(index),
		allOrderedPatches.slice(Math.max(0, index - patchesToIncludeBeforeAndAfterMain._1), index),
		allOrderedPatches.slice(index + 1, Math.min(allOrderedPatches.length, index + 1 + patchesToIncludeBeforeAndAfterMain._2)))
}

trait FixPatchDriver {
	def fix(patch: Patch, patchesBefore: List[Patch] = Nil, patchesAfterwards: List[Patch] = Nil): Patch
}