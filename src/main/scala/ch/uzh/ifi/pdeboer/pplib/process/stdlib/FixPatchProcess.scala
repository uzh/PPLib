package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.patterns.{FixVerifyFPDriver, FixPatchExecuter}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, Patch}

/**
 * Created by pdeboer on 14/12/14.
 */
class FixPatchProcess(params: Map[String, Any] = Map.empty) extends ProcessStub[List[Patch], List[Patch]] {

	import FixPatchProcess._

	override protected def run(dataToFix: List[Patch]): List[Patch] = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		val allData = ALL_DATA.get ::: dataToFix.filter(d => !ALL_DATA.get.contains(d))

		val indicesToFix: List[Int] = allData.zipWithIndex.filter(d => dataToFix.contains(d._1)).map(_._2)

		val fixerProcess: PassableProcessParam[Patch, Patch] = FIXER_PROCESS.get
		val targetParamToPassAllData = TARGET_PARAMETER_TO_PASS_ALL_DATA.get
		if (targetParamToPassAllData.isDefined) {
			fixerProcess.params += targetParamToPassAllData.get.key -> ALL_DATA.get
		}
		val driver = new FixVerifyFPDriver(fixerProcess)
		val exec = new FixPatchExecuter(driver, allData, indicesToFix)
		exec.allFixedPatches.map(_._2)
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(ALL_DATA, TARGET_PARAMETER_TO_PASS_ALL_DATA,
		TARGET_PARAMETER_TO_PASS_PATCHES_AFTER, TARGET_PARAMETER_TO_PASS_PATCHES_BEFORE, PATCHES_TO_INCLUDE_BEFORE_AND_AFTER_MAIN)

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(FIXER_PROCESS)
}

object FixPatchProcess {
	val ALL_DATA = new ProcessParameter[List[Patch]]("allData", OtherParam(), Some(List(Nil)))
	val TARGET_PARAMETER_TO_PASS_PATCHES_BEFORE = new ProcessParameter[Option[ProcessParameter[List[Patch]]]]("targetParamToPassPatchesBefore", OtherParam(), Some(List(None)))
	val TARGET_PARAMETER_TO_PASS_PATCHES_AFTER = new ProcessParameter[Option[ProcessParameter[List[Patch]]]]("targetParamToPassPatchesAfter", OtherParam(), Some(List(None)))
	val TARGET_PARAMETER_TO_PASS_ALL_DATA = new ProcessParameter[Option[ProcessParameter[List[Patch]]]]("targetParamToPassPatchesAllData", OtherParam(), Some(List(Some(FixPatchProcess.ALL_DATA))))
	val PATCHES_TO_INCLUDE_BEFORE_AND_AFTER_MAIN = new ProcessParameter[(Int, Int)]("patchesToIncludeBeforeAndAfterMain", OtherParam(), Some(List((2, 2))))
	val FIXER_PROCESS = new ProcessParameter[PassableProcessParam[Patch, Patch]]("fixerProcess", WorkflowParam(), None)
}