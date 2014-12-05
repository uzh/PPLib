package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.{WorkflowParam, ProcessParameter, ProcessStub}

/**
 * Created by pdeboer on 05/12/14.
 */
class SelectPruneProcess(params: Map[String, Any] = Map.empty) extends ProcessStub[List[String], List[String]](params) {

	import SelectPruneProcess._

	override protected def run(data: List[String]): List[String] = {
		val selections = SELECT.get.process(data)
		PRUNE.get.process(selections)
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(SELECT, PRUNE)
}

object SelectPruneProcess {
	val SELECT = new ProcessParameter[ProcessStub[List[String], List[String]]]("select", WorkflowParam(), None)
	val PRUNE = new ProcessParameter[ProcessStub[List[String], List[String]]]("prune", WorkflowParam(), None)
}
