package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.{ProcessStub, NoProcessMemoizer, ProcessMemoizer}
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

/**
 * Created by pdeboer on 14/12/14.
 */
class FixPatchExecuter(@transient var driver: FixPatchDriver,
					   val allOrderedPatches: List[Patch],
					   val indicesOfPatchesToFix: List[Int],
					   val patchesToIncludeBeforeAndAfterMain: (Int, Int) = (2, 2),
					   @transient var memoizer: ProcessMemoizer = new NoProcessMemoizer(),
					   val memoizerPrefix: String = "") extends Serializable with LazyLogger {

	lazy val allFixedPatches: List[(Int, Patch)] = {
		indicesOfPatchesToFix.mpar.map(i => (i,
			memoizer.mem(memoizerPrefix + "fixpatch" + i)(getFixForPatchAtIndex(i))
			)).toList
	}

	lazy val allPatches: List[Patch] = {
		logger.info("fixing patches")
		allOrderedPatches.zipWithIndex.map(p => {
			val possibleFix: Option[Patch] = allFixedPatches.find(_._1 == p._2).map(_._2)
			possibleFix.getOrElse(p._1)
		})
	}

	def getFixForPatchAtIndex(index: Int) = driver.fix(allOrderedPatches(index),
		allOrderedPatches.slice(Math.max(0, index - patchesToIncludeBeforeAndAfterMain._1), index),
		allOrderedPatches.slice(index + 1, Math.min(allOrderedPatches.length, index + 1 + patchesToIncludeBeforeAndAfterMain._2)))
}

trait FixPatchDriver {
	def fix(patch: Patch, patchesBefore: List[Patch] = Nil, patchesAfterwards: List[Patch] = Nil): Patch
}

class FixVerifyFPDriver(val process: PassableProcessParam[Patch, Patch]) extends FixPatchDriver with LazyLogger {
	override def fix(patch: Patch, patchesBefore: List[Patch], patchesAfterwards: List[Patch]): Patch = {
		logger.info(s"Fixing patch $patch with before: ${patchesBefore.mkString(",")} and after ${patchesAfterwards.mkString(",")}")

		val memPrefixInParams: String = process.getParam[Option[String]](
			ProcessStub.MEMOIZER_NAME.key).getOrElse(Some("")).getOrElse("")
		val higherPriorityParams = Map(ProcessStub.MEMOIZER_NAME.key -> Some(memPrefixInParams.hashCode + "fixprocess"))
		val fixProcess = process.create(higherPrioParams = higherPriorityParams)
		fixProcess.process(patch)
	}
}