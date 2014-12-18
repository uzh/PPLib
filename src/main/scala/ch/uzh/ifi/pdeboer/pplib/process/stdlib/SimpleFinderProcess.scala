package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompInstructionsWithTupleStringified
import ch.uzh.ifi.pdeboer.pplib.patterns.SimpleFinder
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch

/**
 * Created by pdeboer on 05/12/14.
 */
@PPLibProcess("decide.prune.simplefinder")
class SimpleFinderProcess(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[List[Patch], List[Patch]](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.SimpleFinderProcess._

	override protected def run(data: List[Patch]): List[Patch] = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())
		val finder = new SimpleFinder(data, QUESTION.get, TITLE.get, FINDERS_PER_ITEM.get,
			SHUFFLE.get, portal, MAX_ITEMS_PER_FIND.get, memoizer)

		finder.result.filter(_._2 >= THRESHOLD_TO_KEEP_ITEM.get).map(_._1).toList
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(QUESTION, TITLE, FINDERS_PER_ITEM, MAX_ITEMS_PER_FIND, SHUFFLE, THRESHOLD_TO_KEEP_ITEM)
}

object SimpleFinderProcess {
	val QUESTION = new ProcessParameter[HCompInstructionsWithTupleStringified]("question", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Please select sentences you think are erroneous and should be improved"))))
	val TITLE = new ProcessParameter[String]("title", QuestionParam(), Some(List("Find erroneous sentences")))
	val FINDERS_PER_ITEM = new ProcessParameter[Int]("finders", WorkerCountParam(), Some(List(3)))
	val MAX_ITEMS_PER_FIND = new ProcessParameter[Int]("maxItemsPerFind", OtherParam(), Some(List(5)))
	val SHUFFLE = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))
	val THRESHOLD_TO_KEEP_ITEM = new ProcessParameter[Int]("threshold", OtherParam(), Some(List(2)))
}
