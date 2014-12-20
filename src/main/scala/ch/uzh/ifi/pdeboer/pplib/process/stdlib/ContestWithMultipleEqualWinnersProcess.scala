package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTuple, HCompInstructionsWithTupleStringified}
import ch.uzh.ifi.pdeboer.pplib.patterns.ContestWithMultipleEqualWinners
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch

import scala.xml.NodeSeq

/**
 * Created by pdeboer on 05/12/14.
 */
@PPLibProcess("decide.vote.contestWithMultipleEqualWinners")
class ContestWithMultipleEqualWinnersProcess(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[List[Patch], List[Patch]](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithMultipleEqualWinnersProcess._

	override protected def run(data: List[Patch]): List[Patch] = {
		logger.info("running simple finder on: \n -" + data.map(_.value.replaceAll("\n", "")).mkString("\n -"))
		val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())
		val finder = new ContestWithMultipleEqualWinners(data, QUESTION.get, TITLE.get, WORKERS_TO_ASK_PER_ITEM.get,
			SHUFFLE.get, portal, MAX_ITEMS_PER_ITERATION.get, memoizer, QUESTION_AUX.get)

		val res = finder.result.filter(_._2 >= THRESHOLD_MIN_WORKERS_TO_SELECT_ITEM.get).map(_._1).toList
		logger.info("simple finder selected: \n -" + res.mkString("\n -"))
		res
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(QUESTION, QUESTION_AUX, TITLE, WORKERS_TO_ASK_PER_ITEM, MAX_ITEMS_PER_ITERATION, SHUFFLE, THRESHOLD_MIN_WORKERS_TO_SELECT_ITEM)
}

object ContestWithMultipleEqualWinnersProcess {
	val QUESTION = new ProcessParameter[HCompInstructionsWithTuple]("question", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Please select sentences you think are erroneous and should be improved"))))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", QuestionParam(), Some(List(None)))

	val TITLE = new ProcessParameter[String]("title", QuestionParam(), Some(List("Find erroneous sentences")))
	val WORKERS_TO_ASK_PER_ITEM = new ProcessParameter[Int]("finders", WorkerCountParam(), Some(List(3)))
	val MAX_ITEMS_PER_ITERATION = new ProcessParameter[Int]("maxItemsPerFind", OtherParam(), Some(List(10)))
	val SHUFFLE = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))
	val THRESHOLD_MIN_WORKERS_TO_SELECT_ITEM = new ProcessParameter[Int]("threshold", OtherParam(), Some(List(2)))
}
