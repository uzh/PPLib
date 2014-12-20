package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.util.Random
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 31/10/14.
 */
@PPLibProcess("decide.vote.contest")
class Contest(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[Patch], Patch](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Contest._

	override def run(alternatives: List[Patch]): Patch = {
		if (alternatives.size == 0) null
		else if (alternatives.size == 1) alternatives(0)
		else {
			val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w =>
				memoizer.mem("it" + w)(
					U.retry(2) {
						portal.sendQueryAndAwaitResult(
							createMultipleChoiceQuestion(alternatives.map(_.toString).toSet.toList, QUESTION.get, INSTRUCTION_ITALIC.get, TITLE.get),
							PRICE_PER_VOTE.get
						) match {
							case Some(a: MultipleChoiceAnswer) => a.selectedAnswer
							case _ => {
								logger.info(s"${getClass.getSimpleName} didn't get answer for query. retrying..")
								throw new IllegalStateException("didnt get any response")
							}
						}
					}
				)).toList

			val valueOfAnswer: String = answers.groupBy(s => s).maxBy(s => s._2.size)._1
			logger.info("got answer " + valueOfAnswer)
			alternatives.find(_.value == valueOfAnswer).get
		}

	}

	def createMultipleChoiceQuestion(alternatives: List[String], instructions: HCompInstructionsWithTuple, auxString: String, title: String): MultipleChoiceQuery = {
		val choices = if (SHUFFLE_CHOICES.get) Random.shuffle(alternatives) else alternatives
		new MultipleChoiceQuery(instructions.getInstructions(auxString, htmlData = QUESTION_AUX.get.getOrElse(Nil)), choices, 1, 1, title)
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(INSTRUCTION_ITALIC,
			QUESTION_AUX, TITLE, QUESTION, WORKER_COUNT, PRICE_PER_VOTE) ::: super.optionalParameters
}

object Contest {
	val QUESTION = new ProcessParameter[HCompInstructionsWithTuple]("question", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Please select the sentence that fits best in terms of writing style, grammar and low mistake count", questionAfterTuples = "Please do not accept more than 1 HIT in this group."))))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", QuestionParam(), Some(List(None)))
	val INSTRUCTION_ITALIC = new ProcessParameter[String]("auxString", QuestionParam(), Some(List("")))
	val SHUFFLE_CHOICES = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))
	val TITLE = new ProcessParameter[String]("title", QuestionParam(), Some(List("Select the sentence that fits best")))
	val WORKER_COUNT = new ProcessParameter[Int]("workerCount", WorkerCountParam(), Some(List(3)))
	val PRICE_PER_VOTE = new ProcessParameter[HCompQueryProperties]("pricePerVote", OtherParam(), Some(List(HCompQueryProperties(3))))
}