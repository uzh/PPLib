package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, PassableProcessParam, Patch}

/**
 * Created by pdeboer on 22/12/14.
 */
class FindFixPatchProcess(_params: Map[String, Any] = Map.empty) extends ProcessStub[List[IndexedPatch], List[IndexedPatch]](_params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FindFixPatchProcess._

	override protected def run(data: List[IndexedPatch]): List[IndexedPatch] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())

		val find = FIND_PROCESS.get.create(higherPrioParams = Map(ProcessStub.MEMOIZER_NAME.key -> getMemoizerForProcess(FIND_PROCESS.get, "finder")))
		val fixer = FIX_PROCESS.get.create(Map(FixPatchProcess.ALL_DATA.key -> data), Map(ProcessStub.MEMOIZER_NAME.key -> getMemoizerForProcess(FIND_PROCESS.get, "fixer")))

		val found = memoizer.mem("find")(find.process(data))
		val fixed: Map[Int, IndexedPatch] = memoizer.mem("fix")(fixer.process(found))
			.asInstanceOf[List[IndexedPatch]]
			.map(f => (f.index, f)).toMap

		data.map(d => fixed.get(d.index).getOrElse(d))
	}

	def getMemoizerForProcess(process: PassableProcessParam[List[Patch], List[Patch]], suffix: String = "") = {
		val memPrefixInParams: String = process.getParam[Option[String]](
			ProcessStub.MEMOIZER_NAME.key).getOrElse(Some("")).getOrElse("")
		Some(memPrefixInParams.hashCode + suffix)
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(FIND_PROCESS, FIX_PROCESS)
}

object FindFixPatchProcess {
	val FIND_PROCESS = new ProcessParameter[PassableProcessParam[List[Patch], List[Patch]]]("findProcess", WorkflowParam(), None)
	val FIX_PROCESS = new ProcessParameter[PassableProcessParam[List[Patch], List[Patch]]]("fixProcess", WorkflowParam(), None)
}
