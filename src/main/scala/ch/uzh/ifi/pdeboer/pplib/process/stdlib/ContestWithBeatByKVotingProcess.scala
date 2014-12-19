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
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())
		var globalIteration: Int = 0
		val stringData = data.map(_.value)

		do {
			getCrowdWorkers(delta).foreach(w => {
				val answer = memoizer.mem("it" + w + "global" + globalIteration)(
					portal.sendQueryAndAwaitResult(createMultipleChoiceQuestion(stringData),
						HCompQueryProperties(3)).get.asInstanceOf[MultipleChoiceAnswer].selectedAnswer)
				synchronized {
					votes += answer -> votes.getOrElse(answer, 0)
				}
			})
			globalIteration += 1
		} while (shouldStartAnotherIteration)

		val winner = bestAndSecondBest._1._1
		data.find(d => d.value == winner).get
	}

	def shouldStartAnotherIteration: Boolean = {
		delta < K.get && votes.values.sum + delta < MAX_VOTES.get
	}

	def delta = if (votes.size == 0) 3 else Math.abs(bestAndSecondBest._1._2 - bestAndSecondBest._2._2)

	def bestAndSecondBest = {
		val sorted = votes.toList.sortBy(-_._2)
		(sorted(0), sorted(1))
	}

	def createMultipleChoiceQuestion(alternatives: List[String]): MultipleChoiceQuery = {
		val choices = if (SHUFFLE_CHOICES.get) Random.shuffle(alternatives) else alternatives
		new MultipleChoiceQuery(QUESTION.get.getInstructions(INSTRUCTION_ITALIC.get, htmlData = QUESTION_AUX.get.getOrElse(Nil)), choices, 1, 1, TITLE.get)
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(QUESTION_AUX, QUESTION, K) ::: super.optionalParameters
}

object ContestWithBeatByKVotingProcess {
	val TITLE = new ProcessParameter[String]("title", QuestionParam(), Some(List("Select the sentence that fits best")))
	val QUESTION = new ProcessParameter[HCompInstructionsWithTupleStringified]("question", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Please select the sentence that fits best in terms of writing style, grammar and low mistake count", questionAfterTuples = "Please do not accept more than 1 HIT in this group."))))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", QuestionParam(), Some(List(None)))
	val INSTRUCTION_ITALIC = new ProcessParameter[String]("auxString", QuestionParam(), Some(List("")))
	val K = new ProcessParameter[Int]("k", OtherParam(), Some(List(2)))
	val MAX_VOTES = new ProcessParameter[Int]("maxVotes", OtherParam(), Some(List(20)))
	val SHUFFLE_CHOICES = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))
}
