package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.process.entities.PatchConversion._

import scala.xml.NodeSeq

/**
 * Created by pdeboer on 01/12/14.
 */
@PPLibProcess("create.refine.collection")
class Collection(params: Map[String, Any] = Map.empty) extends ProcessStubWithHCompPortalAccess[Patch, List[Patch]](params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Collection._

	override protected def run(line: Patch): List[Patch] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(line.hashCode() + "").getOrElse(new NoProcessMemoizer())

		val answers: List[String] = memoizer.mem("answer_line_" + line) {
			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {
				val questionPerLine: HCompInstructionsWithTuple = QUESTION.get
				val instructions: String = questionPerLine.getInstructions(line + "", htmlData = QUESTION_AUX.get.getOrElse(Nil))
				portal.sendQueryAndAwaitResult(FreetextQuery(
					instructions, "", TITLE_PER_QUESTION.get + w), QUESTION_PRICE.get).get.is[FreetextAnswer]
			}).toList

			answers.map(_.is[FreetextAnswer].answer).toSet.toList
		}
		answers.map(a => line.duplicate(a))
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(QUESTION_AUX, QUESTION_PRICE, TITLE_PER_QUESTION, QUESTION, WORKER_COUNT) ::: super.optionalParameters
}
object Collection {
	val QUESTION = new ProcessParameter[HCompInstructionsWithTuple]("question", QuestionParam(), Some(List(HCompInstructionsWithTupleStringified("Please refine the following sentence:", questionAfterTuples = "Your answer will be evaluated by other crowd workers and an artificial intelligence. Malicious answers will get rejected, so please don't just submit a copy&paste of the original text"))))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", QuestionParam(), Some(List(None)))
	val TITLE_PER_QUESTION = new ProcessParameter[String]("title", QuestionParam(), Some(List("Please refine the following sentence")))
	val WORKER_COUNT = new ProcessParameter[Int]("workerCount", WorkerCountParam(), Some(List(3)))
	val QUESTION_PRICE = new ProcessParameter[HCompQueryProperties]("cost", OtherParam(), Some(List(HCompQueryProperties())))
}