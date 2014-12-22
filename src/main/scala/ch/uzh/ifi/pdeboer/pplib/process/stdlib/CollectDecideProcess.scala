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
		val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())

		logger.info("Running collect-phase for patch")
		val collection: List[Patch] = memoizer.mem("collectProcess")(COLLECT.get.create(params).process(data))
		val collectionDistinct = collection.distinct
		logger.info(s"got ${collection.length} results. ${collectionDistinct.length} after pruning. Running decide")
		val res = memoizer.mem(getClass.getSimpleName + "decideProcess")(DECIDE.get.create(params).process(collectionDistinct))
		logger.info(s"Collect/decide for $res has finished with Patch $res")
		res
	}
}

object CollectDecideProcess {
	val COLLECT = new ProcessParameter[PassableProcessParam[Patch, List[Patch]]]("collect", WorkflowParam(), None)
	val DECIDE = new ProcessParameter[PassableProcessParam[List[Patch], Patch]]("decide", WorkflowParam(), None)
}
