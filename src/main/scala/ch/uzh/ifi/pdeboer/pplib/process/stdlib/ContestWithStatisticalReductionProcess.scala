package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.util.{MonteCarlo, U}

/**
 * Created by pdeboer on 03/11/14.
 */
@PPLibProcess
class ContestWithStatisticalReductionProcess(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Patch], Patch](params) with HCompPortalAccess with InstructionHandler with HCompQueryBuilderSupport[List[Patch]] {

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithStatisticalReductionProcess._

	protected val MONTECARLO_ITERATIONS: Int = 100000
	protected var votesCast = scala.collection.mutable.Map.empty[String, Int]

	override protected def run(data: List[Patch]): Patch = {
		if (data.isEmpty) null
		else if (data.size == 1) data.head
		else {
			val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())
			var iteration: Int = 0
			data.foreach(d => votesCast += (d.value -> 0))
			do {
				iteration += 1
				val choice: String = memoizer.mem("it" + iteration)(castVote(data, iteration))
				votesCast += choice -> (votesCast.getOrElse(choice, 0) + 1)
			} while (minVotesForAgreement(data).getOrElse(Integer.MAX_VALUE) > itemWithMostVotes._2 && votesCast.values.sum < MAX_ITERATIONS.get)

			val winner = itemWithMostVotes._1
			logger.info(s"contest with statistical reduction finished after $iteration rounds. Winner: $winner")
			data.find(d => d.value == winner).get
		}
	}

	def itemWithMostVotes: (String, Int) = {
		votesCast.maxBy(_._2)
	}

	protected def minVotesForAgreement(data: List[Patch]): Option[Int] = {
		MonteCarlo.minAgreementRequired(data.size, votesCast.values.sum, confidence, MONTECARLO_ITERATIONS)
	}

	def castVote(choices: List[Patch], iteration: Int): String = {
		U.retry(3) {
			portal.sendQueryAndAwaitResult(
				queryBuilder.buildQuery("", choices, this),
				QUESTION_PRICE.get

			) match {
				case Some(a: HCompAnswer) => queryBuilder.parseAnswer[String]("", choices, a, this).get
				case _ =>
					logger.info(getClass.getSimpleName + " didnt get a vote when asked for it.")
					throw new IllegalStateException("didnt get any response")
			}
		}
	}

	protected def confidence = CONFIDENCE_PARAMETER.get

	override val processParameterDefaults: Map[ProcessParameter[_], List[Any]] = {
		Map(queryBuilderParam -> List(new DefaultMCQueryBuilder()))
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(CONFIDENCE_PARAMETER, SHUFFLE_CHOICES, MAX_ITERATIONS) ::: super.optionalParameters

	override def getCostCeiling(data: List[Patch]): Int = MAX_ITERATIONS.get * QUESTION_PRICE.get.paymentCents

}

object ContestWithStatisticalReductionProcess {
	val CONFIDENCE_PARAMETER = new ProcessParameter[Double]("confidence", Some(List(0.9d)))
}
