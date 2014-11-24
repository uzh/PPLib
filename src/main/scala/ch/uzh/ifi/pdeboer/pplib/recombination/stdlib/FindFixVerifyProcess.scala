package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompInstructionsWithTuple, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.patterns.{FindFixVerifyExecutor, FFVDefaultHCompDriver, FFVPatch}
import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.FindFixVerifyProcess._
import ch.uzh.ifi.pdeboer.pplib.recombination.{PPLibProcess, ProcessStubWithHCompPortalAccess, ProcessParameter, ProcessStub}

import scala.concurrent.duration._

/**
 * Created by pdeboer on 04/11/14.
 */
@PPLibProcess("create.refine.findfixverify")
class FindFixVerifyProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[String], List[String]](params) {
	override protected def run(data: List[String]): List[String] = {
		val driver = new FFVDefaultHCompDriver(
			data.zipWithIndex.map(d => FFVPatch[String](d._1, d._2)),
			portal, getParamUnsafe(FIND_QUESTION), getParamUnsafe(FIX_QUESTION),
			getParamUnsafe(FIND_TITLE), getParamUnsafe(FIX_TITLE), getParamUnsafe(VERIFY_PROCESS)
		)

		val exec = new FindFixVerifyExecutor(
			driver, getParamUnsafe(PATCHES_COUNT_IN_FIND), getParamUnsafe(FINDERS_COUNT),
			getParamUnsafe(MIN_FINDERS_TO_AGREE_FOR_FIX), getParamUnsafe(FIXERS_PER_PATCH)
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
	val TIMEOUT = new ProcessParameter[Duration]("timeout", Some(List(2 days)))
	val FIND_QUESTION = new ProcessParameter[String]("findQuestion", Some(List(FFVDefaultHCompDriver.DEFAULT_FIND_QUESTION)))
	val FIND_TITLE = new ProcessParameter[String]("findTitle", Some(List(FFVDefaultHCompDriver.DEFAULT_FIND_TITLE)))
	val FIX_TITLE = new ProcessParameter[String]("fixTitle", Some(List(FFVDefaultHCompDriver.DEFAULT_FIX_TITLE)))
	val FIX_QUESTION = new ProcessParameter[HCompInstructionsWithTuple]("fixQuestion", Some(List(FFVDefaultHCompDriver.DEFAULT_FIX_QUESTION)))
	val VERIFY_PROCESS = new ProcessParameter[ProcessStub[List[String], String]]("verifyProcess", Some(List(FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS)))

	val PATCHES_COUNT_IN_FIND = new ProcessParameter[Integer]("patchesInFind", Some(List(10)))
	val FINDERS_COUNT = new ProcessParameter[Integer]("findersCount", Some(List(3)))
	val MIN_FINDERS_TO_AGREE_FOR_FIX = new ProcessParameter[Integer]("minFindersToAgree", Some(List(2)))
	val FIXERS_PER_PATCH = new ProcessParameter[Integer]("fixersPerPatch", Some(List(3)))
}