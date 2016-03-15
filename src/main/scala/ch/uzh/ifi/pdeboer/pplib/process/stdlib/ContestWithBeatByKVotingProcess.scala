package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompAnswer
import ch.uzh.ifi.pdeboer.pplib.process.entities._

/**
  * Created by pdeboer on 28/11/14.
  */
@PPLibProcess
class ContestWithBeatByKVotingProcess(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Patch], Patch](params) with HCompPortalAccess with InstructionHandler with HCompQueryBuilderSupport[List[Patch]] {

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess._

	protected val votes = new VotesTable()

	override protected def run(data: List[Patch]): Patch = {
		if (data.size == 1) data.head
		else if (data.isEmpty) null
		else {
			val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())
			var globalIteration: Int = 0
			do {
				logger.info("started iteration " + globalIteration)
				getCrowdWorkers((K.get - delta).toInt).foreach(w => {
					val answerOpt = memoizer.mem(s"vote $globalIteration $w")(obtainValidVote(data))
					answerOpt.foreach(answer => {
						data.synchronized {
							logger.info("got valid vote for " + answer)
							votes.addVote(answer._1, answer._2)
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
				winner
			else
				null
		} else {
			winner
		}
	}

	protected var uncountedVotes: Int = 0

	private def obtainValidVote(data: List[Patch]): Option[(Patch, HCompAnswer)] = {
		val answerRaw = portal.sendQueryAndAwaitResult(createMultipleChoiceQuestion(data),
			QUESTION_PRICE.get).get
		val ans = queryBuilder.parseAnswer[String](data, answerRaw, this)
		val patch = data.find(d => ans.contains(d.value)).orNull

		if (ans.isDefined) ans.map(a => (patch, answerRaw))
		else {
			uncountedVotes += 1
			if (uncountedVotes + votes.votesCount < MAX_ITERATIONS.get)
				obtainValidVote(data)
			else None
		}
	}

	def shouldStartAnotherIteration: Boolean = {
		neededToWin > 0 && votes.votesCount + uncountedVotes + neededToWin <= MAX_ITERATIONS.get
	}

	def neededToWin = K.get - delta

	def delta = if (votes.answers.isEmpty) 0d else Math.abs(bestAndSecondBest._1._2 - bestAndSecondBest._2._2)

	def bestAndSecondBest = {
		val sorted = votes.sortedByWeight.take(2)
		if (sorted.size == 1) (sorted.head, sorted.head)
		else
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
