package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompInstructionsWithTuple, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.patterns._
import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.FindFixVerifyProcess._
import ch.uzh.ifi.pdeboer.pplib.recombination._

import scala.concurrent.duration._

/**
 * Created by pdeboer on 04/11/14.
 */
@PPLibProcess("create.refine.findfixverify")
class FindFixVerifyProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[String], List[String]](params) {
	override protected def run(data: List[String]): List[String] = {
		val driver = new FFVDefaultHCompDriver(
			data.zipWithIndex.map(d => FFVPatch[String](d._1, d._2)),
			portal, FIND_QUESTION.get, FIX_QUESTION.get,
			FIND_TITLE.get, FIX_TITLE.get, VERIFY_PROCESS.get
		)

		val exec = new FindFixVerifyExecutor(
			driver, PATCHES_COUNT_IN_FIND.get, FINDERS_COUNT.get,
			MIN_FINDERS_TO_AGREE_FOR_FIX.get, FIXERS_PER_PATCH.get
		)

		exec.bestPatches.map(_.patch)
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(
		TIMEOUT, FIND_QUESTION,
		FIX_QUESTION, VERIFY_PROCESS,
		FIND_TITLE, FIX_TITLE,

		PATCHES_COUNT_IN_FIND, FINDERS_COUNT,
		MIN_FINDERS_TO_AGREE_FOR_FIX, FIXERS_PER_PATCH
	)
}

object FindFixVerifyProcess {
	val TIMEOUT = new ProcessParameter[Duration]("timeout", OtherParam(), Some(List(2 days)))
	val FIND_QUESTION = new ProcessParameter[FFVFindQuestion]("findQuestion", QuestionParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_FIND_QUESTION)))
	val FIND_TITLE = new ProcessParameter[String]("findTitle", QuestionParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_FIND_TITLE)))
	val FIX_TITLE = new ProcessParameter[String]("fixTitle", QuestionParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_FIX_TITLE)))
	val FIX_QUESTION = new ProcessParameter[FFVFixQuestion]("fixQuestion", QuestionParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_FIX_QUESTION)))
	val VERIFY_PROCESS = new ProcessParameter[ProcessStub[List[String], String]]("verifyProcess", WorkflowParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS)))

	val PATCHES_COUNT_IN_FIND = new ProcessParameter[Integer]("patchesInFind", OtherParam(), Some(List(10)))
	val FINDERS_COUNT = new ProcessParameter[Integer]("findersCount", WorkerCountParam(), Some(List(3)))
	val MIN_FINDERS_TO_AGREE_FOR_FIX = new ProcessParameter[Integer]("minFindersToAgree", OtherParam(), Some(List(2)))
	val FIXERS_PER_PATCH = new ProcessParameter[Integer]("fixersPerPatch", WorkerCountParam(), Some(List(3)))

	val VERIFY_PROCESS_CONTEXT_PARAMETER = new ProcessParameter[Option[ProcessParameter[String]]]("verifyProcessContextParameter", OtherParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS_CONTEXT_PARAMETER)))
	val VERIFY_PROCESS_CONTEXT_FLATTENER = new ProcessParameter[(List[FFVPatch[String]] => String)]("verifyProcessContextParameter", OtherParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS_CONTEXT_FLATTENER)))
}