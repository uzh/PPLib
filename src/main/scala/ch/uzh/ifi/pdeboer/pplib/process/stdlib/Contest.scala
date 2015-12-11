package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.util.U

/**
 * Created by pdeboer on 31/10/14.
 */
@PPLibProcess
class Contest(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Patch], Patch](params) with HCompPortalAccess with InstructionHandler with QueryInjection with HCompQueryBuilderSupport[List[Patch]] {

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

	override def run(alternatives: List[Patch]): Patch = {
		if (alternatives.isEmpty) null
		else if (alternatives.size == 1) alternatives.head
		else {
			val memoizer: ProcessMemoizer = getProcessMemoizer(alternatives.hashCode() + "").getOrElse(new NoProcessMemoizer())

			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w =>
				memoizer.mem("contestit" + w)(
					U.retry(2) {
						val baseQuery = createMultipleChoiceQuestion(alternatives)
						portal.sendQueryAndAwaitResult(
							createComposite(baseQuery),
							QUESTION_PRICE.get
						) match {
							case Some(a: CompositeQueryAnswer) => (a, a.get[HCompAnswer](baseQuery))
							case _ => {
								logger.info(s"${getClass.getSimpleName} didn't get answer for query. retrying..")
								throw new IllegalStateException("didnt get any response")
							}
						}
					}
				)).toList

			val valueOfAnswer: Option[String] = answers.groupBy(s => queryBuilder.parseAnswer[String]("", alternatives, s._2, this)).maxBy(s => s._2.length)._1
			logger.info("got answer " + valueOfAnswer)
			val p = alternatives.find(_.value == valueOfAnswer.get).get
			addInjectedAnswersToPatch(p, answers.map(_._1))
			p
		}

	}

	def createMultipleChoiceQuestion(alternatives: List[Patch]) = {
		queryBuilder.buildQuery("", alternatives, this)
	}

	override val processParameterDefaults: Map[ProcessParameter[_], List[Any]] = {
		Map(queryBuilderParam -> List(new DefaultMCQueryBuilder()))
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(WORKER_COUNT) ::: super.optionalParameters

	override def getCostCeiling(data: List[Patch]): Int = WORKER_COUNT.get * QUESTION_PRICE.get.paymentCents

}