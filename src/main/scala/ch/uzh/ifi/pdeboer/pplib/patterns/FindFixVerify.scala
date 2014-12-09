package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithFixWorkerCountProcess
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.collection.mutable
import scala.collection.parallel.ForkJoinTaskSupport
import scala.util.Random

/**
 * Created by pdeboer on 21/10/14.
 */
@SerialVersionUID(1l) class FindFixVerifyExecutor[T](@transient var driver: FindFixVerifyDriver[T],
							   val maxPatchesCountInFind: Int = 10,
							   val findersCount: Int = 3,
							   val minFindersCountThatNeedToAgreeForFix: Int = 2,
							   val fixersPerPatch: Int = 3,
							   val parallelWorkers: Boolean = true,
							   @transient var memoizer: ProcessMemoizer = new NoProcessMemoizer()) extends Serializable {
	lazy val bestPatches = {
		if (!ran) runUntilConverged()
		allPatches.toArray.map(p => p._2.best.getOrElse(p._2.original)).sortBy(_.patchIndex).toList
	}
	protected val allPatches = driver.orderedPatches.map(p => p.patchIndex -> new FFVPatchContainer[T](p)).toMap
	protected var ran: Boolean = false

	def runUntilConverged(): Unit = {
		ran = true
		val toFix = memoizer.mem("toFix")(findPatches())
		val fixes = memoizer.mem("fixes")(getAlternativesForPatchesToFix(toFix))
		val fixesAdded = memoizer.mem("fixesAdded") {
			addFixesAsAlternativesToAllPatches(fixes)
			""
		}

		val bestPatchesFound = memoizer.mem("bestPatchesFound")(getBestPatchesFromAllPatchesVAR())
		saveBestPatchesToAllPatches(bestPatchesFound)
	}

	protected def saveBestPatchesToAllPatches(bestPatchesFound: List[FFVPatch[T]]) {
		bestPatchesFound.foreach(p => allPatches(p.patchIndex).best = Some(p))
	}

	protected def addFixesAsAlternativesToAllPatches(fixes: List[FFVPatch[T]]) {
		fixes.foreach(e => {
			val container = allPatches.get(e.patchIndex).get
			container.alternatives += e
		})
	}

	protected def getBestPatchesFromAllPatchesVAR(): List[FFVPatch[T]] = {
		allPatches.filter(_._2.alternatives.size > 0).map(p => driver.verify(p._2.original, p._2.alternatives.toList)).toList
	}

	protected def getAlternativesForPatchesToFix(toFix: List[FFVPatch[T]]): List[FFVPatch[T]] = {
		getRange(fixersPerPatch).map(i => toFix.par.map(p => driver.fix(p))).flatten.toList
	}

	private def getRange(to: Int) = if (parallelWorkers) {
		val par = (1 to to).view.par
		par.tasksupport = new ForkJoinTaskSupport(U.hugeForkJoinPool)
		par
	} else (1 to to).view

	protected def findPatches() = {
		var findSteps = new mutable.HashMap[Int, List[FFVPatchContainer[T]]]()
		allPatches.zipWithIndex.foreach(p => {
			val k: Int = p._2 / maxPatchesCountInFind
			val list = p._1._2 :: findSteps.getOrElse(k, List.empty[FFVPatchContainer[T]])
			findSteps += k -> list
		})

		val selectedElementsInFind = getRange(findersCount).map(l => findSteps.par.map(p => driver.find(p._2.map(_.original)))).flatten.flatten
		selectedElementsInFind.foreach(e => {
			val container = allPatches.get(e.patchIndex).get //should exist except if driver messes with index. out of scope for us
			container.finders += 1
		})

		allPatches.filter(_._2.finders >= minFindersCountThatNeedToAgreeForFix).map(_._2.original).toList
	}

	@SerialVersionUID(1l) protected class FFVPatchContainer[E](val original: FFVPatch[E],
										 var finders: Int = 0,
										 var alternatives: collection.mutable.ListBuffer[FFVPatch[E]] = new collection.mutable.ListBuffer[FFVPatch[E]](),
										 var best: Option[FFVPatch[E]] = None) extends Serializable

}

trait FindFixVerifyDriver[T] {
	def orderedPatches: List[FFVPatch[T]]

	/**
	 * given a list of patches, return all patches a single crowd worker selected as imperfect.
	 * This method will be called repeatedly
	 * @param patches
	 * @return
	 */
	def find(patches: List[FFVPatch[T]]): List[FFVPatch[T]]

	/**
	 * use a single crowd worker to fix this patch. If working with strings, you may
	 * want to show crowd workers context to that patch
	 * @param patch
	 * @return fixed version of that patch, that will be shown as an alternative in the next step
	 */
	def fix(patch: FFVPatch[T]): FFVPatch[T]

	/**
	 * all alternatives collected for a single patch in the previous step are evaluated here
	 * and a best version is selected. Consider banning people who participated in the "Fix"-step
	 * from this step.
	 * @param patch
	 * @param alternatives
	 */
	def verify(patch: FFVPatch[T], alternatives: List[FFVPatch[T]]): FFVPatch[T]
}

@SerialVersionUID(1l) case class FFVPatch[T](patch: T, patchIndex: Int) extends Serializable

object FFVDefaultHCompDriver {
	val DEFAULT_FIND_QUESTION = new FFVFindQuestion("Please select sentences you think are erroneous and should be improved")
	val DEFAULT_FIND_TITLE = "Find erroneous sentences"

	val DEFAULT_FIX_QUESTION = new FFVFixQuestion("Other crowd workers have agreed on this sentence being erroneous. Please fix it")
	val DEFAULT_FIX_TITLE = "Please fix these sentences"

	val DEFAULT_VERIFY_TITLE = "Choose the best sentence"
	val DEFAULT_VERIFY_QUESTION = HCompInstructionsWithTuple("Other crowd workers have come up with the following alternatives for the sentence below. Please select the one you think works best")
	val DEFAULT_VERIFY_PROCESS = new ContestWithFixWorkerCountProcess(Map(
		ContestWithFixWorkerCountProcess.INSTRUCTIONS.key -> FFVDefaultHCompDriver.DEFAULT_VERIFY_QUESTION,
		ContestWithFixWorkerCountProcess.TITLE.key -> FFVDefaultHCompDriver.DEFAULT_VERIFY_TITLE,
		ContestWithFixWorkerCountProcess.WORKER.key -> 3
	))

	val DEFAULT_VERIFY_PROCESS_CONTEXT_PARAMETER: Option[ProcessParameter[String]] = None
	val DEFAULT_VERIFY_PROCESS_CONTEXT_FLATTENER: (List[FFVPatch[String]] => String) = _.mkString(".")
}

class FFVFindQuestion(val question: String) {
	def fullQuestion(allPatches: List[FFVPatch[String]]) = question
}

class FFVFixQuestion(val question: String) {
	def fullQuestion(patch: FFVPatch[String], allPatches: List[FFVPatch[String]]) =
		HCompInstructionsWithTuple(question).getInstructions(patch.patch)
}

class FFVFixQuestionInclOtherPatches(val questionBeforePatch: String, val questionAfterPatch: String = "",
									 val questionAfterList: String = "",
									 val allDataDisplayFunction: (List[FFVPatch[String]] => String) = l => l.mkString(".")) extends FFVFixQuestion(questionBeforePatch) {
	override def fullQuestion(patch: FFVPatch[String], allPatches: List[FFVPatch[String]]): String =
		HCompInstructionsWithTuple(questionBeforePatch, questionBetweenTuples = questionAfterPatch, questionAfterTuples = questionAfterList)
			.getInstructions(patch.patch, allDataDisplayFunction(allPatches))
}


class FFVDefaultHCompDriver(
							   val orderedPatches: List[FFVPatch[String]],
							   val portal: HCompPortalAdapter,
							   val findQuestion: FFVFindQuestion = FFVDefaultHCompDriver.DEFAULT_FIND_QUESTION,
							   val fixQuestion: FFVFixQuestion = FFVDefaultHCompDriver.DEFAULT_FIX_QUESTION,
							   val findTitle: String = FFVDefaultHCompDriver.DEFAULT_FIND_TITLE,
							   val fixTitle: String = FFVDefaultHCompDriver.DEFAULT_FIX_TITLE,
							   val verifyProcess: ProcessStub[List[String], String] = FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS,
							   val verifyProcessContextParameter: Option[ProcessParameter[String]] = FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS_CONTEXT_PARAMETER,
							   val verifyProcessContextFlattener: (List[FFVPatch[String]] => String) = FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS_CONTEXT_FLATTENER,
							   val shuffleMultipleChoiceQueries: Boolean = true
							   ) extends FindFixVerifyDriver[String] {

	if (verifyProcess.getParamByKey[HCompPortalAdapter]("portal").isEmpty) {
		verifyProcess.params += "portal" -> portal
	}

	//payments taken from http://eprints.soton.ac.uk/372107/1/aaai15-budgetfix.pdf

	override def find(patches: List[FFVPatch[String]]): List[FFVPatch[String]] = {
		val choices = if (shuffleMultipleChoiceQueries) Random.shuffle(orderedPatches) else orderedPatches
		val res = portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(
				findQuestion.fullQuestion(choices),
				patches.map(_.patch),
				-1, 1, findTitle), HCompQueryProperties(3))
			.get.asInstanceOf[MultipleChoiceAnswer]

		res.selectedAnswers.map(a => {
			patches.find(_.patch.equals(a)).get
		})
	}

	override def fix(patch: FFVPatch[String]): FFVPatch[String] = {
		val res = portal.sendQueryAndAwaitResult(
			FreetextQuery(fixQuestion.fullQuestion(patch, orderedPatches), "", fixTitle),
			HCompQueryProperties(6)
		).get.asInstanceOf[FreetextAnswer]

		FFVPatch[String](res.answer, patch.patchIndex)
	}


	override def verify(patch: FFVPatch[String], alternatives: List[FFVPatch[String]]): FFVPatch[String] = {
		if (verifyProcessContextParameter.isDefined)
			verifyProcess.params += verifyProcessContextParameter.get.key -> verifyProcessContextFlattener(orderedPatches)

		val result = verifyProcess.process(alternatives.map(_.patch))

		FFVPatch[String](result, patch.patchIndex)
	}
}