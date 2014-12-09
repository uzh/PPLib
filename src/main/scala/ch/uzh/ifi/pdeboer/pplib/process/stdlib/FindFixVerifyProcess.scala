package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.patterns._
import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FindFixVerifyProcess._

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
			FIND_TITLE.get, FIX_TITLE.get, VERIFY_PROCESS.get,
			VERIFY_PROCESS_CONTEXT_PARAMETER.get, VERIFY_PROCESS_CONTEXT_FLATTENER.get,
			SHUFFLE_CHOICES.get
		)

		val memoizer: ProcessMemoizer = processMemoizer.getOrElse(new NoProcessMemoizer())
		val exec = memoizer.memWithReinitialization("ffvexec")(new FindFixVerifyExecutor(
			driver, PATCHES_COUNT_IN_FIND.get, FINDERS_COUNT.get,
			MIN_FINDERS_TO_AGREE_FOR_FIX.get, FIXERS_PER_PATCH.get,
			memoizer = memoizer
		))(exec => {
			exec.driver = driver
			exec.memoizer = memoizer
			exec
		})

		exec.bestPatches.map(_.patch)
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(
		TIMEOUT, FIND_QUESTION,
		FIX_QUESTION, VERIFY_PROCESS,
		FIND_TITLE, FIX_TITLE,

		PATCHES_COUNT_IN_FIND, FINDERS_COUNT,
		MIN_FINDERS_TO_AGREE_FOR_FIX, FIXERS_PER_PATCH, SHUFFLE_CHOICES,

		VERIFY_PROCESS_CONTEXT_PARAMETER, VERIFY_PROCESS_CONTEXT_FLATTENER
	)
}

object FindFixVerifyProcess {
	val TIMEOUT = new ProcessParameter[Duration]("timeout", OtherParam(), Some(List(14 days)))
	val FIND_QUESTION = new ProcessParameter[FFVFindQuestion]("findQuestion", QuestionParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_FIND_QUESTION)))
	val FIND_TITLE = new ProcessParameter[String]("findTitle", QuestionParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_FIND_TITLE)))
	val FIX_TITLE = new ProcessParameter[String]("fixTitle", QuestionParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_FIX_TITLE)))
	val FIX_QUESTION = new ProcessParameter[FFVFixQuestion]("fixQuestion", QuestionParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_FIX_QUESTION)))
	val VERIFY_PROCESS = new ProcessParameter[ProcessStub[List[String], String]]("verifyProcess", WorkflowParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS)))

	val PATCHES_COUNT_IN_FIND = new ProcessParameter[Integer]("patchesInFind", OtherParam(), Some(List(10)))
	val FINDERS_COUNT = new ProcessParameter[Integer]("findersCount", WorkerCountParam(), Some(List(3)))
	val MIN_FINDERS_TO_AGREE_FOR_FIX = new ProcessParameter[Integer]("minFindersToAgree", OtherParam(), Some(List(2)))
	val FIXERS_PER_PATCH = new ProcessParameter[Integer]("fixersPerPatch", WorkerCountParam(), Some(List(3)))
	val SHUFFLE_CHOICES = new ProcessParameter[Boolean]("shuffle", OtherParam(), Some(List(true)))

	val VERIFY_PROCESS_CONTEXT_PARAMETER = new ProcessParameter[Option[ProcessParameter[String]]]("verifyProcessContextParameter", OtherParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS_CONTEXT_PARAMETER)))
	val VERIFY_PROCESS_CONTEXT_FLATTENER = new ProcessParameter[(List[FFVPatch[String]] => String)]("verifyProcessContextFlattener", OtherParam(), Some(List(FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS_CONTEXT_FLATTENER)))
}