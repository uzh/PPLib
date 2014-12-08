package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextAnswer, FreetextQuery, HCompInstructionsWithTuple, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.patterns.SigmaPruner
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithSigmaPruning._

/**
 * Created by pdeboer on 01/12/14.
 */
@PPLibProcess("create.refine.contestwithsixsigma")
class ContestWithSigmaPruning(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[List[String], List[String]](params) {
	override protected def run(data: List[String]): List[String] = {
		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())

		data.map(line => {
			val answerTextsWithinSigmas = memoizer.mem("answer_line_" + line) {
				val answers = getCrowdWorkers(ANSWERS_TO_COLLECT_PER_LINE.get).map(w => {
					val questionPerLine: HCompInstructionsWithTuple = QUESTION_PER_LINE.get
					portal.sendQueryAndAwaitResult(FreetextQuery(
						questionPerLine.getInstructions(line)), HCompQueryProperties(4)).get.asInstanceOf[FreetextAnswer]
				})

				val pruner = new SigmaPruner(
					answers.map(_.processingTimeMillis.toDouble).toList,
					NUM_SIGMAS.get)

				val answersWithinSigmas = answers.filter(a => {
					val procTime: Double = a.processingTimeMillis.toDouble
					procTime >= pruner.minAllowedValue && procTime <= pruner.maxAllowedValue
				})

				logger.info(s"pruned ${answers.size - answersWithinSigmas.size} answers")

				answersWithinSigmas.map(_.answer).toList
			}

			val selectionProcess: ProcessStub[List[String], String] = SELECTION_PROCESS.get
			selectionProcess.params += ProcessStub.MEMOIZER_NAME.key -> processMemoizer.map(_.name + "_selection")
			selectionProcess.process(answerTextsWithinSigmas.toList)
		})
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(QUESTION_PER_LINE, SELECTION_PROCESS, NUM_SIGMAS, ANSWERS_TO_COLLECT_PER_LINE)
}

object ContestWithSigmaPruning {
	val QUESTION_PER_LINE = new ProcessParameter[HCompInstructionsWithTuple]("questionPerLine", QuestionParam(), Some(List(HCompInstructionsWithTuple("Please refine the following sentence"))))
	val SELECTION_PROCESS = new ProcessParameter[ProcessStub[List[String], String]]("selectionProcess", WorkflowParam(), Some(List(new ContestWithFixWorkerCountProcess())))
	val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", OtherParam(), Some(List(3)))
	val ANSWERS_TO_COLLECT_PER_LINE = new ProcessParameter[Int]("answersToCollectPerLine", OtherParam(), Some(List(5)))
}