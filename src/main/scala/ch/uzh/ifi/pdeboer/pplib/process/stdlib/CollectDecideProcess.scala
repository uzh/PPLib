package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, Patch}

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
		val collection: List[Patch] = memoizer.mem("collectProcess")(COLLECT.get.create(if (FORWARD_PARAMS_TO_COLLECT.get) params else Map.empty).process(data))
		val collectionDistinct = collection.distinct
		logger.info(s"got ${collection.length} results. ${collectionDistinct.length} after pruning. Running decide")
		val res = memoizer.mem(getClass.getSimpleName + "decideProcess")(DECIDE.get.create(if (FORWARD_PARAMS_TO_DECIDE.get) params else Map.empty).process(collectionDistinct))
		logger.info(s"Collect/decide for $res has finished with Patch $res")
		res
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(FORWARD_PARAMS_TO_COLLECT, FORWARD_PARAMS_TO_DECIDE)
}

object CollectDecideProcess {
	val FORWARD_PARAMS_TO_COLLECT = new ProcessParameter[Boolean]("forwardParamsToCollect", OtherParam(), Some(List(true)))
	val FORWARD_PARAMS_TO_DECIDE = new ProcessParameter[Boolean]("forwardParamsToDecide", OtherParam(), Some(List(true)))
	val COLLECT = new ProcessParameter[PassableProcessParam[Patch, List[Patch]]]("collect", WorkflowParam(), None)
	val DECIDE = new ProcessParameter[PassableProcessParam[List[Patch], Patch]]("decide", WorkflowParam(), None)
}
