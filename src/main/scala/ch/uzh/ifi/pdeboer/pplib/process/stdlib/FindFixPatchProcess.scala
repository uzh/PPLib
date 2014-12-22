package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.{WorkflowParam, ProcessParameter, ProcessStub}

/**
 * Created by pdeboer on 22/12/14.
 */
class FindFixPatchProcess(_params: Map[String, Any] = Map.empty) extends ProcessStub[List[Patch], List[Patch]](_params) {

	import FindFixPatchProcess._

	override protected def run(data: List[Patch]): List[Patch] = {
		val find = FIND_PROCESS.get.create()
		val fixer = FIX_PROCESS.get.create(Map(FixPatchProcess.ALL_DATA.key -> data))

		val found = find.process(data)
		fixer.process(found)
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(FIND_PROCESS, FIX_PROCESS)
}

object FindFixPatchProcess {
	val FIND_PROCESS = new ProcessParameter[PassableProcessParam[List[Patch], List[Patch]]]("findProcess", WorkflowParam(), None)
	val FIX_PROCESS = new ProcessParameter[PassableProcessParam[List[Patch], List[Patch]]]("fixProcess", WorkflowParam(), None)
}
