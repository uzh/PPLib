package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.recombination.{ProcessParameter, ProcessStub, WorkflowParam}

/**
 * Created by pdeboer on 05/12/14.
 */
class CollectPruneDecideProcess(params: Map[String, Any] = Map.empty) extends ProcessStub[String, String](params) {

	import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.CollectPruneDecideProcess._

	override protected def run(data: String): String = {
		val collection = COLLECT.get.process(data)
		val prunedCollection = PRUNE.get.process(collection)
		DECIDE.get.process(prunedCollection)
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] =
		List(COLLECT, DECIDE).asInstanceOf[List[ProcessParameter[_]]]


	override def optionalParameters: List[ProcessParameter[_]] = List(PRUNE)

	override protected def processCategoryNames: List[String] = List("create.refine.collectprunedecide")
}

object CollectPruneDecideProcess {
	val COLLECT = new ProcessParameter[ProcessStub[String, List[String]]]("collect", WorkflowParam(), None)
	val PRUNE = new ProcessParameter[ProcessStub[List[String], List[String]]]("prune", WorkflowParam(), Some(List(new IdleProcess[List[String]]())))
	val DECIDE = new ProcessParameter[ProcessStub[List[String], String]]("decide", WorkflowParam(), None)
}
