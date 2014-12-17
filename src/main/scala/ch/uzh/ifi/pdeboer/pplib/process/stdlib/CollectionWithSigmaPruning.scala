package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.SigmaPruner
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._
import ch.uzh.ifi.pdeboer.pplib.process.entities.PatchConversion._

/**
 * Created by pdeboer on 01/12/14.
 */
@PPLibProcess("create.refine.collectionwithsixsigma")
class CollectionWithSigmaPruning(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[Patch, List[Patch]](params) {
	override protected def run(line: Patch): List[Patch] = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		val answerTextsWithinSigmas = memoizer.mem("answer_line_" + line) {
			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {
				val questionPerLine: HCompInstructionsWithTupleStringified = QUESTION.get
				portal.sendQueryAndAwaitResult(FreetextQuery(
					questionPerLine.getInstructions(line + ""), "", TITLE_PER_QUESTION.get + w), QUESTION_PRICE.get).get.is[FreetextAnswer]
			}).toList

			val answersWithinSigmas: List[HCompAnswer] = new SigmaPruner(NUM_SIGMAS.get).prune(answers)
			logger.info(s"pruned ${answers.size - answersWithinSigmas.size} answers")

			answersWithinSigmas.map(_.is[FreetextAnswer].answer).toList
		}
		answerTextsWithinSigmas.map(a => line.duplicate(a))
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(QUESTION_PRICE, TITLE_PER_QUESTION, QUESTION, NUM_SIGMAS, WORKER_COUNT) ::: super.optionalParameters
}

object CollectionWithSigmaPruning {
	val QUESTION = new ProcessParameter[HCompInstructionsWithTupleStringified]("question", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Please refine the following sentence:", questionAfterTuples = "Your answer will be evaluated by other crowd workers and an artificial intelligence. Malicious answers will get rejected"))))
	val TITLE_PER_QUESTION = new ProcessParameter[String]("title", QuestionParam(), Some(List("Please refine the following sentence")))
	val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", OtherParam(), Some(List(3)))
	val WORKER_COUNT = new ProcessParameter[Int]("workerCount", WorkerCountParam(), Some(List(3)))
	val QUESTION_PRICE = new ProcessParameter[HCompQueryProperties]("cost", OtherParam(), Some(List(HCompQueryProperties())))
}