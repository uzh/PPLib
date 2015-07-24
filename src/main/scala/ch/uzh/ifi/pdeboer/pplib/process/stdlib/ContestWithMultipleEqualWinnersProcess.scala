package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.patterns.ContestWithMultipleEqualWinners
import ch.uzh.ifi.pdeboer.pplib.process.entities._

/**
 * Created by pdeboer on 05/12/14.
 */
@PPLibProcess
class ContestWithMultipleEqualWinnersProcess(params: Map[String, Any] = Map.empty) extends DecideProcess[List[Patch], List[Patch]](params) with HCompPortalAccess with InstructionHandler {

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithMultipleEqualWinnersProcess._

	override protected def run(data: List[Patch]): List[Patch] = {
		logger.info("running simple finder on: \n -" + data.map(_.value.replaceAll("\n", "")).mkString("\n -"))
		val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())
		val finder = new ContestWithMultipleEqualWinners(data, instructions, instructionTitle, WORKER_COUNT.get,
			SHUFFLE_CHOICES.get, portal, MAX_ITEMS_PER_ITERATION.get, memoizer, QUESTION_AUX.get, QUESTION_PRICE.get, MAX_ITERATIONS.get)

		val res = finder.result.filter(_._2 >= THRESHOLD_MIN_WORKERS_TO_SELECT_ITEM.get).map(_._1).toList
		logger.info("simple finder selected: \n -" + res.mkString("\n -"))
		res
	}

	override def getCostCeiling: Int = MAX_ITERATIONS.get * QUESTION_PRICE.get.paymentCents

	override def optionalParameters: List[ProcessParameter[_]] = List(MAX_ITERATIONS, SHUFFLE_CHOICES, WORKER_COUNT, MAX_ITEMS_PER_ITERATION, THRESHOLD_MIN_WORKERS_TO_SELECT_ITEM)
}

object ContestWithMultipleEqualWinnersProcess {
	val MAX_ITEMS_PER_ITERATION = new ProcessParameter[Int]("maxItemsPerFind", Some(List(10)))
	val THRESHOLD_MIN_WORKERS_TO_SELECT_ITEM = new ProcessParameter[Int]("threshold", Some(List(2)))
}
