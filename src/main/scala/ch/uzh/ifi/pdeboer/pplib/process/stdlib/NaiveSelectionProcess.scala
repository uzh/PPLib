package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompInstructionsWithTuple
import ch.uzh.ifi.pdeboer.pplib.patterns.NaiveFinder
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.NaiveSelectionProcess._

/**
 * Created by pdeboer on 05/12/14.
 */
class NaiveSelectionProcess(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[List[Patch], List[Patch]](params) {
	override protected def run(data: List[Patch]): List[Patch] = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())
		val finder = new NaiveFinder(data, QUESTION.get, TITLE.get, FINDERS_PER_ITEM.get,
			SHUFFLE.get, portal, MAX_ITEMS_PER_FIND.get, memoizer)

		finder.selected.map(s => (1 to s._2).map(p => s._1)).flatten.toList
	}
}

object NaiveSelectionProcess {
	val QUESTION = new ProcessParameter[HCompInstructionsWithTuple]("question", QuestionParam(), Some(List(HCompInstructionsWithTuple("Please select sentences you think are erroneous and should be improved"))))
	val TITLE = new ProcessParameter[String]("title", QuestionParam(), Some(List("Find erroneous sentences")))
	val FINDERS_PER_ITEM = new ProcessParameter[Int]("finders", WorkerCountParam(), Some(List(3)))
	val MAX_ITEMS_PER_FIND = new ProcessParameter[Int]("maxItemsPerFind", OtherParam(), Some(List(5)))
	val SHUFFLE = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))
}
