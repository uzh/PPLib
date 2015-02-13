package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.entities.PatchConversion._
import ch.uzh.ifi.pdeboer.pplib.process.entities._

import scala.util.Random

/**
 * Created by pdeboer on 01/12/14.
 */
@PPLibProcess
class Collection(params: Map[String, Any] = Map.empty) extends CreateProcess[Patch, List[Patch]](params) with HCompPortalAccess with InstructionHandler with QueryInjection {

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

	override protected def run(line: Patch): List[Patch] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(line.hashCode() + "").getOrElse(new NoProcessMemoizer())

		memoizer.mem("answer_line_" + line) {
			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {
				val instr: String = instructions.getInstructions(line + "", htmlData = QUESTION_AUX.get.getOrElse(Nil))
				val mainQuery: FreetextQuery = FreetextQuery(
					instr, "", instructionTitle + w + "_" + Math.abs(Random.nextInt()))
				val query = createComposite(mainQuery)
				val answ = portal.sendQueryAndAwaitResult(query, QUESTION_PRICE.get).get.is[CompositeQueryAnswer]
				(answ, answ.get[FreetextAnswer](mainQuery))
			}).toList

			answers.map(a => {
				val newPatch = line.duplicate(a._2.answer)
				addInjectedAnswersToPatch(newPatch, a._1)
				newPatch
			})
		}
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(WORKER_COUNT) ::: super.optionalParameters
}