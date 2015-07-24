package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.util.Random

/**
 * Created by pdeboer on 31/10/14.
 */
@PPLibProcess
class Contest(params: Map[String, Any] = Map.empty[String, Any]) extends DecideProcess[List[Patch], Patch](params) with HCompPortalAccess with InstructionHandler with QueryInjection {

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

	override def run(alternatives: List[Patch]): Patch = {
		if (alternatives.size == 0) null
		else if (alternatives.size == 1) alternatives(0)
		else {
			val memoizer: ProcessMemoizer = getProcessMemoizer(alternatives.hashCode() + "").getOrElse(new NoProcessMemoizer())

			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w =>
				memoizer.mem("contestit" + w)(
					U.retry(2) {
						val baseQuery: MultipleChoiceQuery = createMultipleChoiceQuestion(alternatives.map(_.toString).toSet.toList)
						portal.sendQueryAndAwaitResult(
							createComposite(baseQuery),
							QUESTION_PRICE.get
						) match {
							case Some(a: CompositeQueryAnswer) => (a, a.get[MultipleChoiceAnswer](baseQuery))
							case _ => {
								logger.info(s"${getClass.getSimpleName} didn't get answer for query. retrying..")
								throw new IllegalStateException("didnt get any response")
							}
						}
					}
				)).toList

			val valueOfAnswer = answers.groupBy(s => s._2.selectedAnswer).maxBy(s => s._2.length)._1
			logger.info("got answer " + valueOfAnswer)
			val p = alternatives.find(_.value == valueOfAnswer).get
			addInjectedAnswersToPatch(p, answers.map(_._1))
			p
		}

	}

	def createMultipleChoiceQuestion(alternatives: List[String]): MultipleChoiceQuery = {
		val choices = if (SHUFFLE_CHOICES.get) Random.shuffle(alternatives) else alternatives
		new MultipleChoiceQuery(instructions.getInstructions("", htmlData = QUESTION_AUX.get.getOrElse(Nil)), choices, 1, 1, instructionTitle + " " + Math.abs(Random.nextDouble()))
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(WORKER_COUNT) ::: super.optionalParameters

	override def getCostCeiling: Int = WORKER_COUNT.get * QUESTION_PRICE.get.paymentCents

}