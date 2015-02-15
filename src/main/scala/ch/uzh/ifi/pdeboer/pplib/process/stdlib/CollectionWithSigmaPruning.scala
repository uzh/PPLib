package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.{SigmaCalculator, SigmaPruner}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.PatchConversion._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{Patch, ProcessParameter}

import scala.util.Random

/**
 * Created by pdeboer on 01/12/14.
 */
@PPLibProcess
class CollectionWithSigmaPruning(params: Map[String, Any] = Map.empty) extends CreateProcess[Patch, List[Patch]](params) with HCompPortalAccess with InstructionHandler {

	import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters._
	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._
	override protected def run(patch: Patch): List[Patch] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(patch.hashCode() + "").getOrElse(new NoProcessMemoizer())
		logger.info("running contest with sigma pruning for patch " + patch)

		val answerTextsWithinSigmas: List[String] = memoizer.mem("answer_line_" + patch) {
			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {

				val instructionString: String = instructions.getInstructions(patch + "", htmlData = QUESTION_AUX.get.getOrElse(Nil))
				portal.sendQueryAndAwaitResult(FreetextQuery(
					instructionString, "", instructionTitle + w + "_" + Math.abs(Random.nextInt())), QUESTION_PRICE.get).get.is[FreetextAnswer]
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

	override def optionalParameters: List[ProcessParameter[_]] = List(PRUNE_TEXT_LENGTH, NUM_SIGMAS, WORKER_COUNT) ::: super.optionalParameters
}

object CollectionWithSigmaPruning {
	val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", Some(List(3)))
	val PRUNE_TEXT_LENGTH = new ProcessParameter[Boolean]("pruneByTextLength", Some(List(true)))
}