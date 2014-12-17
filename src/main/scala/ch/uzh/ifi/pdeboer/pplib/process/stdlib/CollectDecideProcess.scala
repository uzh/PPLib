package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities.{Patch, PassableProcessParam}
import ch.uzh.ifi.pdeboer.pplib.process._

/**
 * Created by pdeboer on 05/12/14.
 */
class CollectDecideProcess(_params: Map[String, Any] = Map.empty) extends ProcessStub[Patch, Patch](_params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectDecideProcess._

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] =
		List(COLLECT, DECIDE).asInstanceOf[List[ProcessParameter[_]]]


	override protected def processCategoryNames: List[String] = List("create.refine.collectdecide")

	override protected def run(data: Patch): Patch = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		val collection: List[Patch] = memoizer.mem("collectProcess")(COLLECT.get.create(params).process(data))
		memoizer.mem(getClass.getSimpleName + "decideProcess")(DECIDE.get.create(params).process(collection))
	}
}

object CollectDecideProcess {
	val COLLECT = new ProcessParameter[PassableProcessParam[Patch, List[Patch]]]("collect", WorkflowParam(), None)
	val DECIDE = new ProcessParameter[PassableProcessParam[List[Patch], Patch]]("decide", WorkflowParam(), None)
}
