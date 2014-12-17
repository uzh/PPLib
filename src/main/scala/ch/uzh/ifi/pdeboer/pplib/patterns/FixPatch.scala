package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTupleStringified, HCompInstructionsWithTupleStringified$}
import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectionWithSigmaPruning
import ch.uzh.ifi.pdeboer.pplib.process.{NoProcessMemoizer, ProcessMemoizer, ProcessParameter, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

/**
 * Created by pdeboer on 14/12/14.
 */
class FixPatchExecuter(@transient var driver: FixPatchDriver,
					   val allOrderedPatches: List[Patch],
					   val indicesOfPatchesToFix: List[Int],
					   val patchesToIncludeBeforeAndAfterMain: (Int, Int) = (2, 2),
					   @transient var memoizer: ProcessMemoizer = new NoProcessMemoizer(),
					   val memoizerPrefix: String = "") extends Serializable with LazyLogger {

	lazy val allFixedPatches: List[(Int, Patch)] = {
		indicesOfPatchesToFix.mpar.map(i => (i,
			memoizer.mem(memoizerPrefix + "fixpatch" + i)(getFixForPatchAtIndex(i))
			)).toList
	}

	lazy val allPatches: List[Patch] = {
		logger.info("fixing patches")
		allOrderedPatches.zipWithIndex.map(p => {
			val possibleFix: Option[Patch] = allFixedPatches.find(_._1 == p._2).map(_._2)
			possibleFix.getOrElse(p._1)
		})
	}

	def getFixForPatchAtIndex(index: Int) = driver.fix(allOrderedPatches(index),
		allOrderedPatches.slice(Math.max(0, index - patchesToIncludeBeforeAndAfterMain._1), index),
		allOrderedPatches.slice(index + 1, Math.min(allOrderedPatches.length, index + 1 + patchesToIncludeBeforeAndAfterMain._2)))
}

trait FixPatchDriver {
	def fix(patch: Patch, patchesBefore: List[Patch] = Nil, patchesAfterwards: List[Patch] = Nil): Patch
}

class FixVerifyFPDriver(val process: PassableProcessParam[Patch, Patch],
						val beforeAfterHandler: FixVerifyFPDriver.FVFPDBeforeAfterHandler = FixVerifyFPDriver.DEFAULT_BEFORE_AFTER_HANDLER) extends FixPatchDriver with LazyLogger {

	override def fix(patch: Patch, patchesBefore: List[Patch], patchesAfterwards: List[Patch]): Patch = {
		logger.info(s"Fixing patch $patch with before: ${patchesBefore.mkString(",")} and after ${patchesAfterwards.mkString(",")}")

		val memPrefixInParams: String = process.getParam[Option[String]](
			ProcessStub.MEMOIZER_NAME.key).getOrElse(Some("")).getOrElse("")

		val higherPriorityParams = Map(
			ProcessStub.MEMOIZER_NAME.key -> Some(memPrefixInParams.hashCode + "fixprocess")
		)

		val fixProcess = process.create(higherPrioParams = higherPriorityParams)
		if (beforeAfterHandler.isDefined) beforeAfterHandler.get.apply(fixProcess, patchesBefore, patchesAfterwards)
		fixProcess.process(patch)
	}
}

object FixVerifyFPDriver {
	type FVFPDBeforeAfterHandler = Option[(ProcessStub[Patch, Patch], List[Patch], List[Patch]) => Unit]

	val DEFAULT_BEFORE_AFTER_HANDLER = beforeAfterInstructions("Please refine the following sentence:", "Your answer will be evaluated by an artificial intelligence and other crowd workers; malicous answers will be rejected.")

	def beforeAfterInstructions(question: String, questionAfter: String = "", targetNameSingular: String = "sentence", targetNamePlural: String = "sentences", joiner: String = ". ", targetField: ProcessParameter[HCompInstructionsWithTupleStringified] = CollectionWithSigmaPruning.QUESTION) = Some((p: ProcessStub[Patch, Patch], before: List[Patch], after: List[Patch]) => {
		val beforeXML = <p>The
			{" " + (if (before.length > 1) targetNamePlural else targetNameSingular) + " "}
			before this
			{targetNameSingular}{if (before.length > 1) " are  " else " is "}
			listed below
			{if (before.length > 1) " according to their order of appearance"}
		</p>.toString + <p>
			<i>
			{before.mkString(joiner)}
			</i>
		</p>.toString
		val afterXML = <p>The
			{" " + (if (after.length > 1) targetNamePlural else targetNameSingular) + " "}
			after this
			{targetNameSingular}{if (after.length > 1) " are " else " is "}
			listed below
			{if (after.length > 1) " according to their order of appearance"}
		</p>.toString + <p>
			<i>
			{after.mkString(joiner)}
			</i>
		</p>.toString

		val q = HCompInstructionsWithTupleStringified(question,
			questionBetweenTuples = "" + (if (before.length > 0) beforeXML) + (if (after.length > 0) afterXML),
			questionAfterTuples = questionAfter)
		p.params += targetField.key -> q
	})
}