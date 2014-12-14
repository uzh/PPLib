package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.SigmaPruner
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

/**
 * Created by pdeboer on 01/12/14.
 */
@PPLibProcess("create.refine.collectionwithsixsigma")
class CollectionWithSigmaPruning(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[List[String], List[String]](params) {
	override protected def run(data: List[String]): List[String] = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		data.mpar.map(line => {
			val answerTextsWithinSigmas = memoizer.mem("answer_line_" + line) {
				val answers = getCrowdWorkers(ANSWERS_TO_COLLECT_PER_LINE.get).map(w => {
					val questionPerLine: HCompInstructionsWithTuple = QUESTION_PER_LINE.get
					portal.sendQueryAndAwaitResult(FreetextQuery(
						questionPerLine.getInstructions(line), line, TITLE_PER_QUESTION.get), QUESTION_PRICE.get).get.is[FreetextAnswer]
				}).toList

				val answersWithinSigmas: List[HCompAnswer] = new SigmaPruner(NUM_SIGMAS.get).prune(answers)
				logger.info(s"pruned ${answers.size - answersWithinSigmas.size} answers")

				answersWithinSigmas.map(_.is[FreetextAnswer].answer).toList
			}

			val selectionProcess: ProcessStub[List[String], String] = SELECTION_PROCESS.get
			selectionProcess.params += ProcessStub.MEMOIZER_NAME.key -> processMemoizer.map(_.name + "_selection")
			selectionProcess.process(answerTextsWithinSigmas.toList)
		}).toList
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(QUESTION_PRICE, TITLE_PER_QUESTION, QUESTION_PER_LINE, SELECTION_PROCESS, NUM_SIGMAS, ANSWERS_TO_COLLECT_PER_LINE) ::: super.optionalParameters
}

object CollectionWithSigmaPruning {
	val QUESTION_PER_LINE = new ProcessParameter[HCompInstructionsWithTuple]("questionPerLine", QuestionParam(), Some(List(HCompInstructionsWithTuple("Please refine the following sentence", questionAfterTuples = "Please do not accept more than 1 HIT in this group."))))
	val TITLE_PER_QUESTION = new ProcessParameter[String]("title", QuestionParam(), Some(List("Please refine the following sentence")))
	val SELECTION_PROCESS = new ProcessParameter[ProcessStub[List[String], String]]("selectionProcess", WorkflowParam(), Some(List(new ContestWithFixWorkerCountProcess())))
	val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", OtherParam(), Some(List(3)))
	val ANSWERS_TO_COLLECT_PER_LINE = new ProcessParameter[Int]("answersToCollectPerLine", OtherParam(), Some(List(5)))
	val QUESTION_PRICE = new ProcessParameter[HCompQueryProperties]("costPerLine", OtherParam(), Some(List(HCompQueryProperties())))
}