package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, PassableProcessParam, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.{ProcessParameter, ProcessStubWithHCompPortalAccess}

/**
 * Created by pdeboer on 14/12/14.
 */
class ListScaleProcess(_params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[List[IndexedPatch], List[IndexedPatch]](_params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ListScaleProcess._

	override protected def run(data: List[IndexedPatch]): List[IndexedPatch] = {
		val processType = CHILD_PROCESS.get

		val lowerPriorityParams = params

		data.map(d => {
			val process = processType.create(lowerPriorityParams)
			process.process(d).asInstanceOf[IndexedPatch]
		})
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(CHILD_PROCESS)
}

object ListScaleProcess {
	val CHILD_PROCESS = new ProcessParameter[PassableProcessParam[Patch, Patch]]("childProcess", None)
}
