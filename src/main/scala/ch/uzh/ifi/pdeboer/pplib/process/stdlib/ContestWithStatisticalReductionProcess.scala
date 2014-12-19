package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.util.MonteCarlo

import scala.util.Random
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 03/11/14.
 */
@PPLibProcess("decide.consensus.contestWithConfidence")
class ContestWithStatisticalReductionProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[Patch], Patch](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithStatisticalReductionProcess._

	protected val MONTECARLO_ITERATIONS: Int = 100000
	protected var votesCast = scala.collection.mutable.Map.empty[String, Int]

	override protected def run(data: List[Patch]): Patch = {
		val stringData = data.map(_.value)
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())
		var iteration: Int = 0
		do {
			iteration += 1
			val choice: String = memoizer.mem("it" + iteration)(castVote(stringData, iteration))
			votesCast += choice -> (votesCast.getOrElse(choice, 0) + 1)
		} while (minVotesForAgreement(stringData).getOrElse(Integer.MAX_VALUE) > itemWithMostVotes._2 && votesCast.values.sum < MAX_VOTES.get)

		val winner = itemWithMostVotes._1
		data.find(d => (d.value == winner)).get
	}

	def itemWithMostVotes: (String, Int) = {
		votesCast.maxBy(_._2)
	}

	protected def minVotesForAgreement(data: List[String]): Option[Int] = {
		MonteCarlo.minAgreementRequired(data.size, votesCast.values.sum, confidence, MONTECARLO_ITERATIONS)
	}

	def castVote(choices: List[String], iteration: Int): String = {
		val instructions = QUESTION.get
		val auxString = INSTRUCTION_ITALIC.get
		val title = TITLE_PARAMETER.get
		val alternatives = if (SHUFFLE_CHOICES.get) Random.shuffle(choices) else choices

		portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(
				instructions.getInstructions(auxString, htmlData = QUESTION_AUX.get.getOrElse(Nil)),
				alternatives, 1, 1, title + iteration),
			PRICE_PER_VOTE.get

		) match {
			case Some(a: MultipleChoiceAnswer) => a.selectedAnswer
			case _ => throw new IllegalStateException("didnt get any response") //TODO change me
		}
	}

	protected def confidence = CONFIDENCE_PARAMETER.get

	override val processCategoryNames: List[String] = List("selectbest.statistical")


	override def optionalParameters: List[ProcessParameter[_]] =
		List(INSTRUCTION_ITALIC, QUESTION_AUX, PRICE_PER_VOTE, TITLE_PARAMETER, CONFIDENCE_PARAMETER, SHUFFLE_CHOICES, QUESTION, MAX_VOTES) ::: super.optionalParameters
}

object ContestWithStatisticalReductionProcess {
	val QUESTION = new ProcessParameter[HCompInstructionsWithTupleStringified]("question", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Please select the sentence that fits best in terms of writing style, grammar and low mistake count"))))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", QuestionParam(), Some(List(None)))
	val INSTRUCTION_ITALIC = new ProcessParameter[String]("auxString", QuestionParam(), Some(List("")))
	val SHUFFLE_CHOICES = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))
	val TITLE_PARAMETER = new ProcessParameter[String]("title", QuestionParam(), Some(List("Please select the sentence that fits best ")))
	val MAX_VOTES = new ProcessParameter[Int]("maxVotes", OtherParam(), Some(List(20)))
	val CONFIDENCE_PARAMETER = new ProcessParameter[java.lang.Double]("confidence", OtherParam(), Some(List(0.9d, 0.95d, 0.99d)))
	val PRICE_PER_VOTE = new ProcessParameter[HCompQueryProperties]("pricePerVote", OtherParam(), Some(List(HCompQueryProperties(3))))
}
