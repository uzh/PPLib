package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.{Prunable, SigmaCalculator, SigmaPruner}
import ch.uzh.ifi.pdeboer.pplib.process.entities.PatchConversion._
import ch.uzh.ifi.pdeboer.pplib.process.entities._

import scala.util.Random

/**
 * Created by pdeboer on 01/12/14.
 */
@PPLibProcess
class CollectionWithSigmaPruning(params: Map[String, Any] = Map.empty) extends CreateProcess[Patch, List[Patch]](params) with HCompPortalAccess with InstructionHandler with QueryInjection {

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning._
	override protected def run(patch: Patch): List[Patch] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(patch.hashCode() + "").getOrElse(new NoProcessMemoizer())
		logger.info("running contest with sigma pruning for patch " + patch)

		memoizer.mem("answer_line_" + patch) {
			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {

				val instructionString: String = instructions.getInstructions(patch + "", INSTRUCTIONS_ITALIC.get, htmlData = QUESTION_AUX.get.getOrElse(Nil))
				val mainQuery: FreetextQuery = FreetextQuery(
					instructionString, "", instructionTitle + w + "_" + Math.abs(Random.nextInt()))

				val res = portal.sendQueryAndAwaitResult(createComposite(mainQuery), QUESTION_PRICE.get).get.is[CompositeQueryAnswer]
				PrunableTuple(res, res.get[FreetextAnswer](mainQuery))
			}).toList

			val timeWithinSigma: List[PrunableTuple] = new SigmaPruner(NUM_SIGMAS.get).prune(answers)
			logger.info(s"TIME MEASURE: pruned ${answers.size - timeWithinSigma.size} answers for patch " + patch)

			val withinSigma: List[PrunableTuple] = if (PRUNE_TEXT_LENGTH.get) {
				val calc = new SigmaCalculator(timeWithinSigma.map(_.freetextAnswer.answer.length.toDouble), NUM_SIGMAS.get)
				timeWithinSigma.filter(a => {
					val fta = a.freetextAnswer.answer.length.toDouble
					fta >= calc.minAllowedValue && fta <= calc.maxAllowedValue
				})
			} else timeWithinSigma

			withinSigma.map(a => {
				val p = patch.duplicate(a.freetextAnswer.answer)
				addInjectedAnswersToPatch(p, a.composite)
				p
			})
		}
	}

	private case class PrunableTuple(composite: CompositeQueryAnswer, freetextAnswer: FreetextAnswer) extends Prunable {
		override def prunableDouble: Double = freetextAnswer.prunableDouble
	}

	override def dataSizeMultiplicator = WORKER_COUNT.get

	override def getCostCeiling(data: Patch): Int = WORKER_COUNT.get * QUESTION_PRICE.get.paymentCents
	override def optionalParameters: List[ProcessParameter[_]] = List(PRUNE_TEXT_LENGTH, NUM_SIGMAS, WORKER_COUNT) ::: super.optionalParameters
}

object CollectionWithSigmaPruning {
	val NUM_SIGMAS = new ProcessParameter[Int]("numSigmas", Some(List(3)))
	val PRUNE_TEXT_LENGTH = new ProcessParameter[Boolean]("pruneByTextLength", Some(List(true)))
}