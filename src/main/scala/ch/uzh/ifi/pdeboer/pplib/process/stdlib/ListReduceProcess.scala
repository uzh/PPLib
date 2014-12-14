package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam
import ch.uzh.ifi.pdeboer.pplib.process.{ProcessParameter, ProcessStub, ProcessStubWithHCompPortalAccess, WorkflowParam}

/**
 * Created by pdeboer on 14/12/14.
 */
class ListReduceProcess(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[String, String] {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ListReduceProcess._

	override protected def run(data: String): String = {
		val processType = CHILD_PROCESS.get
		val memoizerPrefix = ProcessStub.MEMOIZER_NAME.get.getOrElse("")
		val memPrefixInParams: String = processType.getParam[Option[String]](
			ProcessStub.MEMOIZER_NAME.key).getOrElse(Some("")).getOrElse("")

		val memoizerName: String = memoizerPrefix + memPrefixInParams
		val memoizer = if (memoizerName == "") None else Some(memoizerName)

		val lowerPriorityParams = Map(ProcessStubWithHCompPortalAccess.PORTAL_PARAMETER.key -> portal)
		val higherPriorityParams = Map(ProcessStub.MEMOIZER_NAME.key -> memoizer)

		val process = processType.create(lowerPriorityParams, higherPriorityParams)
		process.process(List(data))
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(CHILD_PROCESS)
}

object ListReduceProcess {
	val CHILD_PROCESS = new ProcessParameter[PassableProcessParam[List[String], String]]("childProcess", WorkflowParam(), None)
}
