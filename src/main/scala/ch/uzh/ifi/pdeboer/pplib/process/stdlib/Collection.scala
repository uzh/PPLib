package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.PatchConversion._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.{Patch, ProcessParameter}

import scala.util.Random

/**
 * Created by pdeboer on 01/12/14.
 */
@PPLibProcess
class Collection(params: Map[String, Any] = Map.empty) extends CreateProcess[Patch, List[Patch]](params) with HCompPortalAccess with InstructionHandler {

	import ch.uzh.ifi.pdeboer.pplib.process.parameter.DefaultParameters._
	override protected def run(line: Patch): List[Patch] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(line.hashCode() + "").getOrElse(new NoProcessMemoizer())

		val answers: List[String] = memoizer.mem("answer_line_" + line) {
			val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {
				//val questionPerLine: HCompInstructionsWithTuple = instru
				val instr: String = instructions.getInstructions(line + "", htmlData = QUESTION_AUX.get.getOrElse(Nil))
				portal.sendQueryAndAwaitResult(FreetextQuery(
					instr, "", instructionTitle + w + "_" + Math.abs(Random.nextInt())), QUESTION_PRICE.get).get.is[FreetextAnswer]
			}).toList

			answers.map(_.is[FreetextAnswer].answer).toSet.toList
		}
		answers.map(a => line.duplicate(a))
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(QUESTION_AUX, QUESTION_PRICE, INSTRUCTIONS, WORKER_COUNT) ::: super.optionalParameters
}