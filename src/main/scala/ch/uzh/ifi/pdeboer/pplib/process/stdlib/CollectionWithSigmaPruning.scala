package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.{SigmaCalculator, SigmaPruner}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.process.entities.PatchConversion._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._

import scala.util.Random
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 01/12/14.
 */
@PPLibProcess("create.pruned.collectionWithSigmaPruning")
class CollectionWithSigmaPruning(params: Map[String, Any] = Map.empty) extends ProcessStub[Patch, List[Patch]](params) with HCompPortalAccess {
	override protected def run(patch: Patch): List[Patch] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(patch.hashCode() + "").getOrElse(new NoProcessMemoizer())
		logger.info("running contest with sigma pruning for patch " + patch)

		val answerTextsWithinSigmas: List[String] = memoizer.mem("answer_line_" + patch) {
			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {
				val questionPerLine: HCompInstructionsWithTuple = QUESTION.get
				val instructions: String = questionPerLine.getInstructions(patch + "", htmlData = QUESTION_AUX.get.getOrElse(Nil))
				portal.sendQueryAndAwaitResult(FreetextQuery(
					instructions, "", TITLE_PER_QUESTION.get + w + "_" + Math.abs(Random.nextInt())), QUESTION_PRICE.get).get.is[FreetextAnswer]
			}).toList

			val timeWithinSigma: List[HCompAnswer] = new SigmaPruner(NUM_SIGMAS.get).prune(answers)
			logger.info(s"TIME MEASURE: pruned ${answers.size - timeWithinSigma.size} answers for patch " + patch)

			val withinSigma = if (PRUNE_TEXT_LENGTH.get) {
				val calc = new SigmaCalculator(timeWithinSigma.map(_.is[FreetextAnswer].answer.length.toDouble), NUM_SIGMAS.get)
				timeWithinSigma.filter(a => {
					val fta = a.is[FreetextAnswer].answer.length.toDouble
					fta >= calc.minAllowedValue && fta <= calc.maxAllowedValue
				})
			} else timeWithinSigma

			withinSigma.map(a => a.is[FreetextAnswer].answer).toSet.toList
		}
		answerTextsWithinSigmas.map(a => patch.duplicate(a))
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(PRUNE_TEXT_LENGTH, QUESTION_AUX, QUESTION_PRICE, TITLE_PER_QUESTION, QUESTION, NUM_SIGMAS, WORKER_COUNT) ::: super.optionalParameters
}

object CollectionWithSigmaPruning {
	val QUESTION = new ProcessParameter[HCompInstructionsWithTuple]("question", Some(List(HCompInstructionsWithTupleStringified("Please refine the following sentence:", questionAfterTuples = "Your answer will be evaluated by other crowd workers and an artificial intelligence. Malicious answers will get rejected, so please don't just submit a copy&paste of the original text."))))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", Some(List(None)))
	val TITLE_PER_QUESTION = new ProcessParameter[String]("title", Some(List("Please refine the following sentence")))
	val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", Some(List(3)))
	val WORKER_COUNT = new ProcessParameter[Int]("workerCount", Some(List(5, 3)))
	val QUESTION_PRICE = new ProcessParameter[HCompQueryProperties]("cost", Some(List(HCompQueryProperties())))
	val PRUNE_TEXT_LENGTH = new ProcessParameter[Boolean]("pruneByTextLength", Some(List(true)))
}