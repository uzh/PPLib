package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompQuery, HCompAnswer, CompositeQueryAnswer}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.util.Random
import scala.reflect.runtime.universe._

/**
  * Created by pdeboer on 03/03/16.
  */
@PPLibProcess
class BayesianTruthContest(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Patch], Patch](params)
	with HCompPortalAccess with InstructionHandler with QueryInjection with HCompQueryBuilderSupport[List[Patch]] {

	import BayesianTruthContest._

	override protected def run(alternatives: List[Patch]): Patch = {
		if (alternatives.isEmpty) null
		else if (alternatives.size == 1) alternatives.head
		else {
			val memoizer: ProcessMemoizer = getProcessMemoizer(alternatives.hashCode() + "").getOrElse(new NoProcessMemoizer())

			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w =>
				memoizer.mem("bayesianTruth" + w)(
					U.retry(2) {
						val ownOpinion = createMCQueryForOwnOpinion(alternatives)
						def shuffleIfNeeded(l: List[Patch]) = if (SHUFFLE_CHOICES.get) l.sortBy(s => Random.nextDouble()) else l
						val opinionsOnOtherPatches = shuffleIfNeeded(alternatives).map(p => createTextFieldForOthersOpinions(p))
						portal.sendQueryAndAwaitResult(
							createComposite(ownOpinion :: opinionsOnOtherPatches),
							QUESTION_PRICE.get
						) match {
							case Some(a: CompositeQueryAnswer) =>
								val rawOwnAnswer: HCompAnswer = a.get[HCompAnswer](ownOpinion)
								val parsedOwnAnswer: Patch = queryBuilder.parseAnswer[Patch](alternatives, rawOwnAnswer, this).get

								val rawAnswersForOtherPatches = opinionsOnOtherPatches.zipWithIndex.map(pi => alternatives(pi._2) -> a.get[HCompAnswer](pi._1)).toMap
								val parsedAnswersForOtherPatches = rawAnswersForOtherPatches.map(r => r._1 -> otherOpinionsQueryBuilder.parseAnswer[Double](r._1, r._2, this).get)
								BTAnswer(a, parsedOwnAnswer, parsedAnswersForOtherPatches)
							case _ =>
								logger.info(s"${getClass.getSimpleName} didn't get answer for query.")
								throw new IllegalStateException("didnt get any response")
						}
					}
				)).toList

			val prunedAnswers = answers.filter(a => {
				val totalPercentage: Double = a.probabilitiesForOtherAnswers.values.sum
				totalPercentage > 0.97d && totalPercentage < 1.03d
			})
			logger.info(s"pruned ${answers.size - prunedAnswers.size} answers")

			val avgOwnAnswers = alternatives.map(a => a -> (prunedAnswers.count(p => p.ownAnswer == a).toDouble / prunedAnswers.length.toDouble)).toMap
			val avgOthersAnswers = alternatives.map(a => a -> {
				val product = prunedAnswers.foldLeft(1d)((pv, p) => pv * p.probabilitiesForOtherAnswers(a))
				Math.pow(product, 1d / prunedAnswers.size)
			}).toMap

			val userBTSScores = prunedAnswers.map(a => (a, a.btsScore(avgOwnAnswers, avgOthersAnswers)))
			val answerBTS = alternatives.map(p => (p, userBTSScores.filter(_._1.ownAnswer == p).map(b => b._2).sum / avgOwnAnswers(p)))

			val btsSelectedAnswer = answerBTS.maxBy(a => if (a._2.isNaN) 0 else a._2)._1
			val selectedAnswer = if (prunedAnswers.groupBy(_.ownAnswer).size == 1) prunedAnswers.head.ownAnswer else btsSelectedAnswer //trivial case doesnt work with bts

			logger.info(s"selected answer $selectedAnswer")

			addInjectedAnswersToPatch(selectedAnswer, prunedAnswers.map(_.rawAnswer))
			selectedAnswer
		}
	}

	def createMCQueryForOwnOpinion(alternatives: List[Patch]): HCompQuery = {
		queryBuilder.buildQuery(alternatives, this)
	}

	def createTextFieldForOthersOpinions(patch: Patch): HCompQuery = {
		val igForOthersOpinions: InstructionGenerator = nonDefaultInstructionGeneratorOrPool[OtherOpinionsDecide](OTHERS_OPINIONS_INSTRUCTION_GENERATOR)
		otherOpinionsQueryBuilder.buildQuery(patch, this, Some(igForOthersOpinions))
	}


	protected def otherOpinionsQueryBuilder: HCompQueryBuilder[Patch] = OTHERS_OPINIONS_QUERY_BUILDER.get


	override val processParameterDefaults: Map[ProcessParameter[_], List[Any]] = {
		val mergedPool = Map(typeOf[OtherOpinionsDecide] -> new SimpleInstructionGeneratorCreate) ++ INSTRUCTION_GENERATOR_POOL.get
		Map(queryBuilderParam -> List(new DefaultMCQueryBuilder()),
			DefaultParameters.INSTRUCTION_GENERATOR_POOL -> List(mergedPool))
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(WORKER_COUNT, BayesianTruthContest.OTHERS_OPINIONS_INSTRUCTION_GENERATOR, OTHERS_OPINIONS_QUERY_BUILDER) ::: super.optionalParameters

	override def getCostCeiling(data: List[Patch]): Int = WORKER_COUNT.get * QUESTION_PRICE.get.paymentCents


}

private[stdlib] case class BTAnswer(rawAnswer: CompositeQueryAnswer, ownAnswer: Patch, probabilitiesForOtherAnswers: Map[Patch, Double]) {

	def btsScore(avgOwn: Map[Patch, Double], avgOthers: Map[Patch, Double]) = {
		val correctedAvgOwn = avgOwn.map(p => p._1 -> (if (p._2 > 0) p._2 else 0.0001))
		val leftTerm = Math.log(correctedAvgOwn(ownAnswer) / avgOthers(ownAnswer))
		val rightTerm = probabilitiesForOtherAnswers.map(p => correctedAvgOwn(p._1) * Math.log(p._2 / correctedAvgOwn(p._1))).filterNot(d => d.isInfinite || d.isNaN).sum
		leftTerm + rightTerm
	}
}

object BayesianTruthContest {
	val OTHERS_OPINIONS_INSTRUCTION_GENERATOR = new ProcessParameter[InstructionGenerator]("othersOpinionsInstructionGenerator", Some(List(new SimpleInstructionGeneratorEstimateOthers)))
	val OTHERS_OPINIONS_QUERY_BUILDER = new ProcessParameter[HCompQueryBuilder[Patch]]("othersOpinionsQueryBuilder", Some(List(new DefaultPercentageQueryBuilder())))
}

class OtherOpinionsDecide {}