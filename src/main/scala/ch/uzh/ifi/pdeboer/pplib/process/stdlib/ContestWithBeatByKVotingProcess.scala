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
				getCrowdWorkers(K.get - delta).foreach(w => {
					val answerOpt = memoizer.mem(s"vote $globalIteration $w")(obtainValidVote(data))
					answerOpt.foreach(answer => {
						data.synchronized {
							logger.info("got valid vote for " + answer)
							votes += answer -> (votes.getOrElse(answer, 0) + 1)
						}
					})
				})
				globalIteration += 1
			} while (shouldStartAnotherIteration)

			logger.info(s"beat-by-k finished after $globalIteration rounds")

			getEndResult(data)
		}
	}

	def getEndResult(data: List[Patch]): Patch = {
		val winner = bestAndSecondBest._1._1
		if (bestAndSecondBest._1._2 - bestAndSecondBest._2._2 < K.get) {
			if (RETURN_LEADER_IF_MAX_ITERATIONS_REACHED.get)
				data.find(d => d.value == winner).get
			else
				null
		} else {
			data.find(d => d.value == winner).get
		}
	}

	protected var uncountedVotes: Int = 0

	private def obtainValidVote(data: List[Patch]): Option[String] = {
		val answerRaw = portal.sendQueryAndAwaitResult(createMultipleChoiceQuestion(data),
			QUESTION_PRICE.get).get
		val ans = queryBuilder.parseAnswer[String](data, answerRaw, this)

		if (ans.isDefined) ans
		else {
			uncountedVotes += 1
			if (uncountedVotes + votes.values.sum < MAX_ITERATIONS.get)
				obtainValidVote(data)
			else None
		}
	}

	def shouldStartAnotherIteration: Boolean = {
		neededToWin > 0 && votes.values.sum + uncountedVotes + neededToWin <= MAX_ITERATIONS.get
	}

	def neededToWin = K.get - delta

	def delta = if (votes.isEmpty) 0 else Math.abs(bestAndSecondBest._1._2 - bestAndSecondBest._2._2)

	def bestAndSecondBest = {
		val sorted = votes.toList.sortBy(-_._2)
		(sorted.head, sorted(1))
	}

	def createMultipleChoiceQuestion(alternatives: List[Patch]) = {
		queryBuilder.buildQuery(alternatives, this)
	}

	override def getCostCeiling(data: List[Patch]): Int = MAX_ITERATIONS.get * QUESTION_PRICE.get.paymentCents

	override val processParameterDefaults: Map[ProcessParameter[_], List[Any]] = {
		Map(queryBuilderParam -> List(new DefaultMCQueryBuilder()))
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(SHUFFLE_CHOICES, MAX_ITERATIONS, K, INSTRUCTIONS_ITALIC)
}

object ContestWithBeatByKVotingProcess {
	val K = new ProcessParameter[Int]("k", Some(List(2)))
	val RETURN_LEADER_IF_MAX_ITERATIONS_REACHED = new ProcessParameter[Boolean]("returnLeaderIfMaxIterationsReached", Some(List(true)))
}
