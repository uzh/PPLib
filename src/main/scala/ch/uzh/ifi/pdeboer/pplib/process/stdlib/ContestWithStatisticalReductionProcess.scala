package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.util.{MonteCarlo, U}

import scala.util.Random
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 03/11/14.
 */
@PPLibProcess("decide.voteWithStatisticalConfidence")
class ContestWithStatisticalReductionProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[List[Patch], Patch](params) with HCompPortalAccess {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithStatisticalReductionProcess._

	protected val MONTECARLO_ITERATIONS: Int = 100000
	protected var votesCast = scala.collection.mutable.Map.empty[String, Int]

	override protected def run(data: List[Patch]): Patch = {
		if (data.size == 0) null
		else if (data.size == 1) data(0)
		else {
			val stringData = data.map(_.value)
			val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())
			var iteration: Int = 0
			data.foreach(d => votesCast += (d.value -> 0))
			do {
				iteration += 1
				val choice: String = memoizer.mem("it" + iteration)(castVote(stringData, iteration))
				votesCast += choice -> (votesCast.getOrElse(choice, 0) + 1)
			} while (minVotesForAgreement(stringData).getOrElse(Integer.MAX_VALUE) > itemWithMostVotes._2 && votesCast.values.sum < MAX_VOTES.get)

			val winner = itemWithMostVotes._1
			logger.info(s"contest with statistical reduction finished after $iteration rounds. Winner: $winner")
			data.find(d => (d.value == winner)).get
		}
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

		U.retry(3) {
			portal.sendQueryAndAwaitResult(
				MultipleChoiceQuery(
					instructions.getInstructions(auxString, htmlData = QUESTION_AUX.get.getOrElse(Nil)),
					alternatives, 1, 1, title + iteration + "_" + Math.abs(Random.nextInt())),
				PRICE_PER_VOTE.get

			) match {
				case Some(a: MultipleChoiceAnswer) => a.selectedAnswer
				case _ => {
					logger.info(getClass.getSimpleName + " didnt get a vote when asked for it.")
					throw new IllegalStateException("didnt get any response")
				}
			}
		}
	}

	protected def confidence = CONFIDENCE_PARAMETER.get

	override val processCategoryNames: List[String] = List("selectbest.statistical")


	override def optionalParameters: List[ProcessParameter[_]] =
		List(INSTRUCTION_ITALIC, QUESTION_AUX, PRICE_PER_VOTE, TITLE_PARAMETER, CONFIDENCE_PARAMETER, SHUFFLE_CHOICES, QUESTION, MAX_VOTES) ::: super.optionalParameters
}

object ContestWithStatisticalReductionProcess {
	val QUESTION = new ProcessParameter[HCompInstructionsWithTupleStringified]("question", Some(List(HCompInstructionsWithTupleStringified("Please select the sentence that fits best in terms of writing style, grammar and low mistake count"))))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", Some(List(None)))
	val INSTRUCTION_ITALIC = new ProcessParameter[String]("auxString", Some(List("")))
	val SHUFFLE_CHOICES = new ProcessParameter[Boolean]("shuffle", Some(List(true)))
	val TITLE_PARAMETER = new ProcessParameter[String]("title", Some(List("Please select the sentence that fits best ")))
	val MAX_VOTES = new ProcessParameter[Int]("maxVotes", Some(List(20)))
	val CONFIDENCE_PARAMETER = new ProcessParameter[Double]("confidence", Some(List(0.9d, 0.95d, 0.99d)))
	val PRICE_PER_VOTE = new ProcessParameter[HCompQueryProperties]("pricePerVote", Some(List(HCompQueryProperties(3))))
}
