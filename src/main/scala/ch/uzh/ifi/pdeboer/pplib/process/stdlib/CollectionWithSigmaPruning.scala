package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.SigmaPruner
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.process.entities.PatchConversion._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._

import scala.xml.NodeSeq

/**
 * Created by pdeboer on 01/12/14.
 */
@PPLibProcess("create.refine.collectionwithsixsigma")
class CollectionWithSigmaPruning(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[Patch, List[Patch]](params) {
	override protected def run(line: Patch): List[Patch] = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		val answerTextsWithinSigmas = memoizer.mem("answer_line_" + line) {
			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {
				val questionPerLine: HCompInstructionsWithTuple = QUESTION.get
				val instructions: String = questionPerLine.getInstructions(line + "", htmlData = QUESTION_AUX.get.getOrElse(Nil))
				portal.sendQueryAndAwaitResult(FreetextQuery(
					instructions, "", TITLE_PER_QUESTION.get + w), QUESTION_PRICE.get).get.is[FreetextAnswer]
			}).toList

			val answersWithinSigmas: List[HCompAnswer] = new SigmaPruner(NUM_SIGMAS.get).prune(answers)
			logger.info(s"pruned ${answers.size - answersWithinSigmas.size} answers")

			answersWithinSigmas.map(_.is[FreetextAnswer].answer).toList
		}
		answerTextsWithinSigmas.map(a => line.duplicate(a))
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(QUESTION_AUX, QUESTION_PRICE, TITLE_PER_QUESTION, QUESTION, NUM_SIGMAS, WORKER_COUNT) ::: super.optionalParameters
}

object CollectionWithSigmaPruning {
	val QUESTION = new ProcessParameter[HCompInstructionsWithTuple]("question", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Please refine the following sentence:", questionAfterTuples = "Your answer will be evaluated by other crowd workers and an artificial intelligence. Malicious answers will get rejected, so please don't just submit a copy&paste of the original text"))))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", QuestionParam(), Some(List(None)))
	val TITLE_PER_QUESTION = new ProcessParameter[String]("title", QuestionParam(), Some(List("Please refine the following sentence")))
	val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", OtherParam(), Some(List(3)))
	val WORKER_COUNT = new ProcessParameter[Int]("workerCount", WorkerCountParam(), Some(List(3)))
	val QUESTION_PRICE = new ProcessParameter[HCompQueryProperties]("cost", OtherParam(), Some(List(HCompQueryProperties())))
}