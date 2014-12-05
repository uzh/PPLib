package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTuple, HCompQueryProperties, MultipleChoiceAnswer, MultipleChoiceQuery}
import ch.uzh.ifi.pdeboer.pplib.process._

import scala.collection.mutable
import scala.util.Random

/**
 * Created by pdeboer on 28/11/14.
 */
@PPLibProcess("decide.vote.beatbyk")
class BeatByKVotingProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[String], String](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.BeatByKVotingProcess._

	protected var votes = mutable.HashMap.empty[String, Int]

	override protected def run(data: List[String]): String = {
		do {
			getCrowdWorkers(delta).foreach(w => {
				val answer = portal.sendQueryAndAwaitResult(createMultipleChoiceQuestion(data),
					HCompQueryProperties(3)).get.asInstanceOf[MultipleChoiceAnswer].selectedAnswer
				synchronized {
					votes += answer -> votes.getOrElse(answer, 0)
				}
			})
		} while (shouldStartAnotherIteration)

		bestAndSecondBest._1._1
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
		new MultipleChoiceQuery(INSTRUCTIONS.get.getInstructions(AUX_STRING.get), choices, 1, 1, TITLE.get)
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(INSTRUCTIONS, K)
}

object BeatByKVotingProcess {
	val TITLE = new ProcessParameter[String]("title", QuestionParam(), Some(List("Select the sentence that fits best")))
	val INSTRUCTIONS = new ProcessParameter[HCompInstructionsWithTuple]("question", QuestionParam(), Some(List(HCompInstructionsWithTuple("Please select the option that fits best"))))
	val AUX_STRING = new ProcessParameter[String]("auxString", QuestionParam(), Some(List("")))
	val K = new ProcessParameter[Int]("k", OtherParam(), Some(List(2)))
	val MAX_VOTES = new ProcessParameter[Int]("maxVotes", OtherParam(), Some(List(20)))
	val SHUFFLE_CHOICES = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))
}
