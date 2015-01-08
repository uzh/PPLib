package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch

import scala.collection.mutable
import scala.util.Random
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 28/11/14.
 */
@PPLibProcess("decide.vote.contestWithBeatByK")
class ContestWithBeatByKVotingProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[Patch], Patch](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess._

	protected var votes = mutable.HashMap.empty[String, Int]

	override protected def run(data: List[Patch]): Patch = {
		if (data.size == 1) data(0)
		else if (data.size == 0) null
		else {
			data.foreach(d => votes += (d.value -> 0))
			val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())
			var globalIteration: Int = 0
			val stringData = data.map(_.value)
			do {
				logger.info("started iteration " + globalIteration)
				getCrowdWorkers(delta).foreach(w => {
					val answer = portal.sendQueryAndAwaitResult(createMultipleChoiceQuestion(stringData),
						PRICE_PER_VOTE.get).get.asInstanceOf[MultipleChoiceAnswer].selectedAnswer
					logger.info("waiting for lock..")
					stringData.synchronized {
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
		delta < K.get && votes.values.sum + delta < MAX_VOTES.get
	}

	def delta = if (votes.values.sum == 0) 3 else Math.abs(bestAndSecondBest._1._2 - bestAndSecondBest._2._2)

	def bestAndSecondBest = {
		val sorted = votes.toList.sortBy(-_._2)
		(sorted(0), sorted(1))
	}

	def createMultipleChoiceQuestion(alternatives: List[String]): MultipleChoiceQuery = {
		val choices = if (SHUFFLE_CHOICES.get) Random.shuffle(alternatives) else alternatives
		new MultipleChoiceQuery(QUESTION.get.getInstructions(INSTRUCTION_ITALIC.get, htmlData = QUESTION_AUX.get.getOrElse(Nil)), choices, 1, 1, TITLE.get + " " + Math.abs(Random.nextInt()))
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(SHUFFLE_CHOICES, INSTRUCTION_ITALIC, MAX_VOTES, PRICE_PER_VOTE, QUESTION_AUX, QUESTION, K, TITLE)
}

object ContestWithBeatByKVotingProcess {
	val TITLE = new ProcessParameter[String]("title", QuestionParam(), Some(List("Select the sentence that fits best")))
	val QUESTION = new ProcessParameter[HCompInstructionsWithTupleStringified]("question", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Please select the sentence that fits best in terms of writing style, grammar and low mistake count", questionAfterTuples = "Please do not accept more than 1 HIT in this group."))))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", QuestionParam(), Some(List(None)))
	val INSTRUCTION_ITALIC = new ProcessParameter[String]("auxString", QuestionParam(), Some(List("")))
	val K = new ProcessParameter[Int]("k", OtherParam(), Some(List(2)))
	val MAX_VOTES = new ProcessParameter[Int]("maxVotes", OtherParam(), Some(List(20)))
	val SHUFFLE_CHOICES = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))
	val PRICE_PER_VOTE = new ProcessParameter[HCompQueryProperties]("pricePerVote", OtherParam(), Some(List(HCompQueryProperties(3))))

}
