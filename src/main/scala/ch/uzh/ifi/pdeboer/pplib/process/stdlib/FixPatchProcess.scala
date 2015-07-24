package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.patterns.{FixPatchExecuter, FixVerifyFPDriver}
import ch.uzh.ifi.pdeboer.pplib.process.entities._

/**
 * Created by pdeboer on 14/12/14.
 */
@PPLibProcess
class FixPatchProcess(params: Map[String, Any] = Map.empty) extends ProcessStub[List[Patch], List[Patch]](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FixPatchProcess._

	override protected def run(dataToFix: List[Patch]): List[Patch] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(dataToFix.hashCode() + "").getOrElse(new NoProcessMemoizer())

		val allData = ALL_DATA.get ::: dataToFix.filter(d => !ALL_DATA.get.contains(d))

		val indicesToFix: List[Int] = allData.zipWithIndex.filter(d => dataToFix.contains(d._1)).map(_._2)

		val fixerProcess = FIXER_PROCESS.get
		val targetParamToPassAllData = TARGET_PARAMETER_TO_PASS_ALL_DATA.get
		if (targetParamToPassAllData.isDefined) {
			fixerProcess.params += targetParamToPassAllData.get.key -> ALL_DATA.get
		}
		val driver = new FixVerifyFPDriver(fixerProcess, FIXER_BEFORE_AFTER_HANDLER.get)
		val exec = new FixPatchExecuter(driver, allData, indicesToFix, PATCHES_TO_INCLUDE_BEFORE_AND_AFTER_MAIN.get, memoizer)
		exec.allFixedPatches.map(_._2)
	}


	override def optionalParameters: List[ProcessParameter[_]] = List(ALL_DATA, TARGET_PARAMETER_TO_PASS_ALL_DATA,
		FIXER_BEFORE_AFTER_HANDLER, PATCHES_TO_INCLUDE_BEFORE_AND_AFTER_MAIN)

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(FIXER_PROCESS)

	override def getCostCeiling: Int = FIXER_PROCESS.get.create().getCostCeiling
}

object FixPatchProcess {
	val ALL_DATA = new ProcessParameter[List[Patch]]("allData", Some(List(Nil)))
	val TARGET_PARAMETER_TO_PASS_ALL_DATA = new ProcessParameter[Option[ProcessParameter[List[Patch]]]]("targetParamToPassPatchesAllData", Some(List(Some(FixPatchProcess.ALL_DATA))))
	val PATCHES_TO_INCLUDE_BEFORE_AND_AFTER_MAIN = new ProcessParameter[(Int, Int)]("patchesToIncludeBeforeAndAfterMain", Some(List((1, 1))))
	val FIXER_PROCESS = new ProcessParameter[PassableProcessParam[CreateProcess[Patch, Patch]]]("fixerProcess", None)
	val FIXER_BEFORE_AFTER_HANDLER = new ProcessParameter[FixVerifyFPDriver.FVFPDBeforeAfterHandler]("beforeAfterHandler", Some(List(FixVerifyFPDriver.DEFAULT_BEFORE_AFTER_HANDLER)))
}