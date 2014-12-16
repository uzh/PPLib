package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.FindFixVerifyExecutor.FFVPatchContainer
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.{NoPruner, Prunable, Pruner}
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, PassableProcessParam, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithFixWorkerCountProcess
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

import scala.collection.mutable
import scala.util.Random

/**
 * Created by pdeboer on 21/10/14.
 */
@SerialVersionUID(1l)
class FindFixVerifyExecutor[T](
								  _driver: FindFixVerifyDriver[T],
								  val maxPatchesCountInFind: Int = 10,
								  val findersCount: Int = 3,
								  val minFindersCountThatNeedToAgreeForFix: Int = 2,
								  val fixersPerPatch: Int = 3,
								  val parallelWorkers: Boolean = true,
								  _memoizer: ProcessMemoizer = new NoProcessMemoizer()) extends Serializable {
	@transient var driver = _driver
	@transient var memoizer = _memoizer

	protected val allPatches = driver.orderedPatches.map(p => p.patchIndex -> new FFVPatchContainer[T](p)).toMap
	lazy val bestPatches = {
		if (!ran) runUntilConverged()
		allPatches.toArray.map(p => p._2.best.getOrElse(p._2.original)).sortBy(_.patchIndex).toList
	}
	protected var ran: Boolean = false

	def runUntilConverged(): Unit = {
		val toFix = memoizer.mem("toFix")(findPatches())
		val fixes: List[FFVPatch[T]] = memoizer.mem("fixes")(getAlternativesForPatchesToFix(toFix))
		val fixesAdded = memoizer.mem("fixesAdded") {
			addFixesAsAlternativesToAllPatches(fixes)
			""
		}

		val bestPatchesFound = memoizer.mem("bestPatchesFound")(getBestPatchesFromAllPatchesVAR())
		saveBestPatchesToAllPatches(bestPatchesFound)
		ran = true
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
		(1 to fixersPerPatch).mpar.map(i => {
			toFix.mpar.map(p => driver.fix(p))
		}).flatten.toList
	}

	protected def findPatches() = {
		var findSteps = new mutable.HashMap[Int, List[FFVPatchContainer[T]]]()
		allPatches.zipWithIndex.foreach(p => {
			val k: Int = p._2 / maxPatchesCountInFind
			val list = p._1._2 :: findSteps.getOrElse(k, List.empty[FFVPatchContainer[T]])
			findSteps += k -> list
		})

		val selectedElementsInFind = (1 to findersCount).mpar.map(l => findSteps.par.map(p => driver.find(p._2.map(_.original)))).flatten.flatten
		selectedElementsInFind.foreach(e => {
			val container = allPatches.get(e.patchIndex).get //should exist except if driver messes with index. out of scope for us
			container.finders += 1
		})

		allPatches.filter(_._2.finders >= minFindersCountThatNeedToAgreeForFix).map(_._2.original).toList
	}
}

object FindFixVerifyExecutor {

	@SerialVersionUID(1l)
	protected class FFVPatchContainer[E](val original: FFVPatch[E],
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
	 * Fix this patch. If you're working with strings, you may
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

@SerialVersionUID(1l) class FFVPatch[T](val patch: T, val patchIndex: Int) extends Serializable

object FFVPatch {
	def apply[T](patch: T, patchIndex: Int) = new FFVPatch[T](patch, patchIndex)
}

object FFVPatchToPatch {
	implicit def ffvToPatch(p: FFVPatch[_]): IndexedPatch = {
		p.patch match {
			case s: Serializable => new IndexedPatch(p.patch.toString, p.patchIndex, Some(s))
			case _ => new IndexedPatch(p.patch.toString, p.patchIndex)
		}
	}
}

class PrunablePatch[T](original: FFVPatch[T], val answer: HCompAnswer) extends FFVPatch[T](original.patch, original.patchIndex) with Prunable {
	override def prunableDouble: Double = answer.prunableDouble
}

object FFVDefaultHCompDriver {
	val DEFAULT_FIND_QUESTION = new FFVFindQuestion("Please select sentences you think are erroneous and should be improved.")
	val DEFAULT_FIND_TITLE = "Find erroneous sentences"

	val DEFAULT_FIX_QUESTION = new FFVFixQuestion("Other crowd workers have agreed on this sentence being erroneous. Please fix it", "Please also do not accept more than 1 HIT in this group")
	val DEFAULT_FIX_TITLE = "Please fix this sentence"

	val DEFAULT_VERIFY_TITLE = "Choose the best sentence"
	val DEFAULT_VERIFY_QUESTION = HCompInstructionsWithTuple("Other crowd workers have come up with the following alternatives for the sentence below. Please select the one you think works best")
	val DEFAULT_VERIFY_PROCESS = new PassableProcessParam[List[Patch], Patch](classOf[ContestWithFixWorkerCountProcess], Map(
		ContestWithFixWorkerCountProcess.INSTRUCTIONS.key -> FFVDefaultHCompDriver.DEFAULT_VERIFY_QUESTION,
		ContestWithFixWorkerCountProcess.TITLE.key -> FFVDefaultHCompDriver.DEFAULT_VERIFY_TITLE,
		ContestWithFixWorkerCountProcess.WORKER_COUNT.key -> 3
	))

	val DEFAULT_VERIFY_PROCESS_CONTEXT_PARAMETER: Option[ProcessParameter[String]] = None
	val DEFAULT_VERIFY_PROCESS_CONTEXT_FLATTENER: (List[FFVPatch[String]] => String) = _.mkString(".")

	val DEFAULT_PRUNER = new NoPruner()
}

class FFVFindQuestion(val question: String) extends Serializable {
	def fullQuestion(allPatches: List[FFVPatch[String]]) = question
}

class FFVFixQuestion(val question: String, val disclaimer: String = "") extends Serializable {
	def fullQuestion(patch: FFVPatch[String], allPatches: List[FFVPatch[String]]) =
		HCompInstructionsWithTuple(question, questionAfterTuples = disclaimer).getInstructions(patch.patch)
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
							   val verifyProcessParam: PassableProcessParam[List[Patch], Patch] = FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS,
							   val verifyProcessContextParameter: Option[ProcessParameter[String]] = FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS_CONTEXT_PARAMETER,
							   val verifyProcessContextFlattener: (List[FFVPatch[String]] => String) = FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS_CONTEXT_FLATTENER,
							   val shuffleMultipleChoiceQueries: Boolean = true,
							   val pruner: Pruner = FFVDefaultHCompDriver.DEFAULT_PRUNER
							   ) extends FindFixVerifyDriver[String] {


	override def find(patches: List[FFVPatch[String]]): List[FFVPatch[String]] = {
		val choices = if (shuffleMultipleChoiceQueries) Random.shuffle(orderedPatches) else orderedPatches

		val res = portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(
				findQuestion.fullQuestion(choices),
				patches.map(_.patch),
				-1, 1, findTitle), HCompQueryProperties(3))
			.get.is[MultipleChoiceAnswer]

		res.selectedAnswers.map(a => {
			patches.find(_.patch.equals(a)).get
		})
	}

	override def fix(patch: FFVPatch[String]): FFVPatch[String] = {
		val res = portal.sendQueryAndAwaitResult(
			FreetextQuery(fixQuestion.fullQuestion(patch, orderedPatches), "", fixTitle)
		).get.is[FreetextAnswer]

		new PrunablePatch(FFVPatch[String](res.answer, patch.patchIndex), res)
	}


	override def verify(patch: FFVPatch[String], alternatives: List[FFVPatch[String]]): FFVPatch[String] = {
		val memoizerPrefix = patch.patch
		val memPrefixInParams: String = verifyProcessParam.getParam[Option[String]](
			ProcessStub.MEMOIZER_NAME.key).getOrElse(Some("")).getOrElse("")

		val lowerPriorityParams = Map(ProcessStubWithHCompPortalAccess.PORTAL_PARAMETER.key -> portal)
		val higherPriorityParams = Map(ProcessStub.MEMOIZER_NAME.key -> Some("verify" + memoizerPrefix.hashCode + memPrefixInParams))

		val verifyProcess = verifyProcessParam.create(lowerPriorityParams, higherPriorityParams)

		if (verifyProcessContextParameter.isDefined)
			verifyProcess.params += verifyProcessContextParameter.get.key -> verifyProcessContextFlattener(orderedPatches)

		val result: Patch = verifyProcess.process(alternatives.map(p => FFVPatchToPatch.ffvToPatch(p)))

		FFVPatch[String](result.value, patch.patchIndex)
	}

}
