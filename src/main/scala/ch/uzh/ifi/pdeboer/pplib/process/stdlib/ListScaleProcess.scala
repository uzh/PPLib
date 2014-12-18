package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities.{Patch, PassableProcessParam}
import ch.uzh.ifi.pdeboer.pplib.process.{ProcessParameter, ProcessStub, ProcessStubWithHCompPortalAccess, WorkflowParam}

/**
 * Created by pdeboer on 14/12/14.
 */
class ListScaleProcess(_params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[List[Patch], List[Patch]](_params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ListScaleProcess._

	override protected def run(data: List[Patch]): List[Patch] = {
		val processType = CHILD_PROCESS.get
		val memoizerPrefix = ProcessStub.MEMOIZER_NAME.get.getOrElse("")
		val memPrefixInParams: String = processType.getParam[Option[String]](
			ProcessStub.MEMOIZER_NAME.key).getOrElse(Some("")).getOrElse("")

		val memoizerName: String = memoizerPrefix + memPrefixInParams
		val memoizer = if (memoizerName == "") None else Some(memoizerName)

		val lowerPriorityParams = params
		val higherPriorityParams = Map(ProcessStub.MEMOIZER_NAME.key -> memoizer)

		data.map(d => {
			val process = processType.create(lowerPriorityParams, higherPriorityParams)
			process.process(d)
		})
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(CHILD_PROCESS)
}

object ListScaleProcess {
	val CHILD_PROCESS = new ProcessParameter[PassableProcessParam[Patch, Patch]]("childProcess", WorkflowParam(), None)
}
