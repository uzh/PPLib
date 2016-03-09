package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, CompositeQueryAnswer}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.util.U

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
					U.retry(0) {
						val ownOpinion = createMCQueryForOwnOpinion(alternatives)
						val opinionsOnOtherPatches = alternatives.map(p => createTextFieldForOthersOpinions(p))
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
								logger.info(s"${getClass.getSimpleName} didn't get answer for query. retrying..")
								throw new IllegalStateException("didnt get any response")

						}
					}
				)).toList

			val prunedAnswers = answers.filter(a => {
				val totalPercentage: Double = a.probabilitiesForOtherAnswers.values.sum
				totalPercentage > 99 && totalPercentage < 101
			})

			val avgOwnAnswers = alternatives.map(a => a -> (prunedAnswers.count(p => p.ownAnswer == a).toDouble / prunedAnswers.length.toDouble)).toMap
			val avgOthersAnswers = alternatives.map(a => a -> prunedAnswers.map(p => p.probabilitiesForOtherAnswers(a)).sum / 100d).toMap

			val selectedAnswer = prunedAnswers.maxBy(_.btsScore(avgOwnAnswers, avgOthersAnswers)).ownAnswer
			logger.info(s"selected answer $selectedAnswer")

			addInjectedAnswersToPatch(selectedAnswer, prunedAnswers.map(_.rawAnswer))
			selectedAnswer
		}
	}

	private case class BTAnswer(rawAnswer: CompositeQueryAnswer, ownAnswer: Patch, probabilitiesForOtherAnswers: Map[Patch, Double]) {

		def btsScore(avgOwn: Map[Patch, Double], avgOthers: Map[Patch, Double]) = {
			val leftTerm = Math.log(avgOwn(ownAnswer) / avgOthers(ownAnswer))
			val rightTerm = probabilitiesForOtherAnswers.map(p => avgOwn(p._1) * Math.log(p._2 / avgOwn(p._1))).sum
			leftTerm + rightTerm
		}
	}

	def createMCQueryForOwnOpinion(alternatives: List[Patch]) = {
		queryBuilder.buildQuery(alternatives, this)
	}

	def createTextFieldForOthersOpinions(patch: Patch) = {
		otherOpinionsQueryBuilder.buildQuery(patch, this, Some(OTHERS_OPINIONS_INSTRUCTION_GENERATOR.get))
	}


	protected def otherOpinionsQueryBuilder: HCompQueryBuilder[Patch] = OTHERS_OPINIONS_QUERY_BUILDER.get

	override val processParameterDefaults: Map[ProcessParameter[_], List[Any]] = {
		Map(queryBuilderParam -> List(new DefaultMCQueryBuilder()))
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(WORKER_COUNT, BayesianTruthContest.OTHERS_OPINIONS_INSTRUCTION_GENERATOR) ::: super.optionalParameters

	override def getCostCeiling(data: List[Patch]): Int = WORKER_COUNT.get * QUESTION_PRICE.get.paymentCents


}

object BayesianTruthContest {
	val OTHERS_OPINIONS_INSTRUCTION_GENERATOR = new ProcessParameter[InstructionGenerator]("othersOpinionsInstructionGenerator", Some(List(new SimpleInstructionGeneratorEstimateOthers())))
	val OTHERS_OPINIONS_QUERY_BUILDER = new ProcessParameter[HCompQueryBuilder[Patch]]("othersOpinionsQueryBuilder", Some(List(new DefaultDoubleQueryBuilder())))
}