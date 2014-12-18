package ch.uzh.ifi.pdeboer.pplib.examples

import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, PassableProcessParam, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib._
import ch.uzh.ifi.pdeboer.pplib.process.{FileProcessMemoizer, ProcessStub}

/**
 * Created by pdeboer on 14/12/14.
 */
object FindAndFix extends App {
	val data = List("correct1", "correct2", "error1", "correct3").zipWithIndex.map(p => new IndexedPatch(p))
	val findProcess = new SimpleFinderProcess(Map(SimpleFinderProcess.FINDERS_PER_ITEM.key -> 1))
	val fixProcess = new FixPatchProcess(Map(
		FixPatchProcess.ALL_DATA.key -> data,
		FixPatchProcess.FIXER_PROCESS.key -> new PassableProcessParam[Patch, Patch](classOf[CollectDecideProcess], params = Map(
			CollectDecideProcess.COLLECT.key -> new PassableProcessParam[Patch, List[Patch]](classOf[CollectionWithSigmaPruning], Map(
				CollectionWithSigmaPruning.WORKER_COUNT.key -> 2,
				ProcessStub.MEMOIZER_NAME.key -> Some("collectioncreator")
			)),
			CollectDecideProcess.DECIDE.key -> new PassableProcessParam[List[Patch], Patch](classOf[ContestWithFixWorkerCountProcess], Map(
				ContestWithFixWorkerCountProcess.WORKER_COUNT.key -> 1,
				ProcessStub.MEMOIZER_NAME.key -> Some("contest")
			))
		)),
		ProcessStub.MEMOIZER_NAME.key -> Some("collectDecide")
	))

	val memoizer = new FileProcessMemoizer("findfixtest")

	val toFix = memoizer.mem("tofix")(findProcess.process(data))
	val fixed = fixProcess.process(toFix).asInstanceOf[List[IndexedPatch]]

	val fixedPatchesMapByIndex = fixed.map(p => p.index -> p.value).toMap
	println(data.zipWithIndex.map(d => fixedPatchesMapByIndex.get(d._2).getOrElse(d._1)).mkString(","))
}
