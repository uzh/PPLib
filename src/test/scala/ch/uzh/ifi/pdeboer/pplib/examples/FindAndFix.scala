package ch.uzh.ifi.pdeboer.pplib.examples

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, StringPatchWithIndex}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.NaiveSelectionProcess._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib._

/**
 * Created by pdeboer on 14/12/14.
 */
object FindAndFix extends App {
	val data = List("correct1", "correct2", "error1", "correct3").zipWithIndex.map(p => new StringPatchWithIndex(p))
	val findProcess = new NaiveSelectionProcess(Map(FINDERS_PER_ITEM.key -> 2))
	val fixProcess = new FixPatchProcess(Map(
		FixPatchProcess.ALL_DATA.key -> data,
		FixPatchProcess.FIXER_PROCESS.key -> new PassableProcessParam[String, String](classOf[CollectDecideProcess], Map(
			CollectDecideProcess.COLLECT.key -> new PassableProcessParam[String, List[String]](classOf[CollectionWithSigmaPruning], Map(
				CollectionWithSigmaPruning.WORKER_COUNT.key -> 1
			)),
			CollectDecideProcess.DECIDE.key -> new PassableProcessParam[List[String], String](classOf[ContestWithFixWorkerCountProcess], Map(
				ContestWithFixWorkerCountProcess.WORKER_COUNT.key -> 1
			))
		))
	))

	val toFix = findProcess.process(data)
	val fixed = fixProcess.process(toFix).asInstanceOf[List[StringPatchWithIndex]] //ugly

	val fixedPatchesMapByIndex = fixed.map(p => p.index -> p.payload).toMap
	println(data.zipWithIndex.map(d => fixedPatchesMapByIndex.get(d._2).getOrElse(d._1)).mkString(","))
}
