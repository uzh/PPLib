package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam
import ch.uzh.ifi.pdeboer.pplib.process._

/**
 * Created by pdeboer on 05/12/14.
 */
class CollectDecideProcess(params: Map[String, Any] = Map.empty) extends ProcessStub[String, String](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectDecideProcess._

	override protected def run(data: String): String = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		val collection = memoizer.mem("collectProcess")(COLLECT.get.create(params).process(data))
		memoizer.mem("decideProcess")(DECIDE.get.create(params).process(collection))
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] =
		List(COLLECT, DECIDE).asInstanceOf[List[ProcessParameter[_]]]


	override protected def processCategoryNames: List[String] = List("create.refine.collectdecide")
}

object CollectDecideProcess {
	val COLLECT = new ProcessParameter[PassableProcessParam[String, List[String]]]("collect", WorkflowParam(), None)
	val DECIDE = new ProcessParameter[PassableProcessParam[List[String], String]]("decide", WorkflowParam(), None)
}
