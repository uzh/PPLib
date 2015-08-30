package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities._

import scala.collection.mutable

/**
 * Created by pdeboer on 28/11/14.
 */
@PPLibProcess
class ContestWithBeatByKVotingProcess(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Patch], Patch](params) with HCompPortalAccess with InstructionHandler with HCompQueryBuilderSupport[List[Patch]] {
	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess._

	protected var votes = mutable.HashMap.empty[String, Int]

	override protected def run(data: List[Patch]): Patch = {
		if (data.size == 1) data.head
		else if (data.isEmpty) null
		else {
			data.foreach(d => votes += (d.value -> 0))
			val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())
			var globalIteration: Int = 0
			do {
				logger.info("started iteration " + globalIteration)
				getCrowdWorkers(delta).foreach(w => {
					val answerRaw = portal.sendQueryAndAwaitResult(createMultipleChoiceQuestion(data),
						QUESTION_PRICE.get).get
					val answer = queryBuilder.parseAnswer[String]("", data, answerRaw, this).get
					logger.info("waiting for lock..")
					data.synchronized {
						logger.info("got lock. storing vote")
						votes += answer -> votes.getOrElse(answer, 0)
					}
				})
				globalIteration += 1
			} while (shouldStartAnotherIteration)

			val winner = bestAndSecondBest._1._1
			logger.info(s"beat-by-k finished after $globalIteration rounds. Winner: " + winner)
			data.find(d => d.value == winner).get
		}
	}

	def shouldStartAnotherIteration: Boolean = {
		delta < K.get && votes.values.sum + delta < MAX_ITERATIONS.get
	}

	def delta = if (votes.values.sum == 0) 3 else Math.abs(bestAndSecondBest._1._2 - bestAndSecondBest._2._2)

	def bestAndSecondBest = {
		val sorted = votes.toList.sortBy(-_._2)
		(sorted.head, sorted(1))
	}

	def createMultipleChoiceQuestion(alternatives: List[Patch]) = {
		queryBuilder.buildQuery("", alternatives, this)
	}

	override def getCostCeiling(data: List[Patch]): Int = MAX_ITERATIONS.get * QUESTION_PRICE.get.paymentCents

	override val processParameterDefaults: Map[ProcessParameter[_], List[Any]] = {
		Map(queryBuilderParam -> List(new DefaultMCQueryBuilder()))
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(SHUFFLE_CHOICES, MAX_ITERATIONS, K, INSTRUCTIONS_ITALIC)
}

object ContestWithBeatByKVotingProcess {
	val K = new ProcessParameter[Int]("k", Some(List(2)))
}
