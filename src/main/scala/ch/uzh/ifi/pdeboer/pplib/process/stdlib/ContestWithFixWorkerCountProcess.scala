package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process._

import scala.util.Random

/**
 * Created by pdeboer on 31/10/14.
 */
@PPLibProcess("decide.vote.fix")
class ContestWithFixWorkerCountProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[String], String](params) {
	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithFixWorkerCountProcess._

	override def run(alternatives: List[String]): String = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		val answers = getCrowdWorkers(WORKER_COUNT.get).map(w =>
			memoizer.mem("it" + w)(
				portal.sendQueryAndAwaitResult(
					createMultipleChoiceQuestion(alternatives, INSTRUCTIONS.get, AUX_STRING.get, TITLE.get),
					PRICE_PER_VOTE.get
				) match {
					case Some(a: MultipleChoiceAnswer) => a.selectedAnswer
					case _ => throw new IllegalStateException("didnt get any response") //TODO change me
				})).toList

		answers.groupBy(s => s).maxBy(s => s._2.size)._1
	}

	def createMultipleChoiceQuestion(alternatives: List[String], instructions: HCompInstructionsWithTuple, auxString: String, title: String): MultipleChoiceQuery = {
		val choices = if (SHUFFLE_CHOICES.get) Random.shuffle(alternatives) else alternatives
		new MultipleChoiceQuery(instructions.getInstructions(auxString), choices, 1, 1, title)
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(AUX_STRING,
			TITLE, INSTRUCTIONS, WORKER_COUNT, PRICE_PER_VOTE) ::: super.optionalParameters
}

object ContestWithFixWorkerCountProcess {
	val INSTRUCTIONS = new ProcessParameter[HCompInstructionsWithTuple]("question", QuestionParam(), Some(List(HCompInstructionsWithTuple("Please select the sentence that fits best in terms of writing style, grammar and low mistake count", questionAfterTuples = "Please do not accept more than 1 HIT in this group."))))
	val AUX_STRING = new ProcessParameter[String]("auxString", QuestionParam(), Some(List("")))
	val SHUFFLE_CHOICES = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))
	val TITLE = new ProcessParameter[String]("title", QuestionParam(), Some(List("Select the sentence that fits best")))
	val WORKER_COUNT = new ProcessParameter[Int]("workerCount", WorkerCountParam(), Some(List(3)))
	val PRICE_PER_VOTE = new ProcessParameter[HCompQueryProperties]("pricePerVote", OtherParam(), Some(List(HCompQueryProperties(3))))
}