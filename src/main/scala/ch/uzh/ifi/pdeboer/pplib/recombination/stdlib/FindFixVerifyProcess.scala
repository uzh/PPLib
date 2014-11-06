package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompInstructionsWithData, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.patterns.{FindFixVerifyExecutor, FFVDefaultHCompDriver, FFVPatch}
import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.FindFixVerifyProcess._
import ch.uzh.ifi.pdeboer.pplib.recombination.{RecombinationStubWithHCompPortalAccess, RecombinationParameter, RecombinationStub}

import scala.concurrent.duration._

/**
 * Created by pdeboer on 04/11/14.
 */
class FindFixVerifyProcess(params: Map[String, Any]) extends RecombinationStubWithHCompPortalAccess[List[String], List[String]](params) {
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

	override def optionalParameters: List[RecombinationParameter[_]] = List(
		TIMEOUT, FIND_QUESTION,
		FIX_QUESTION, VERIFY_PROCESS,
		FIND_TITLE, FIX_TITLE,

		PATCHES_COUNT_IN_FIND, FINDERS_COUNT,
		MIN_FINDERS_TO_AGREE_FOR_FIX, FIXERS_PER_PATCH
	)
}

object FindFixVerifyProcess {
	val TIMEOUT = new RecombinationParameter[Duration]("timeout", Some(List(2 days)))
	val FIND_QUESTION = new RecombinationParameter[String]("findQuestion", Some(List(FFVDefaultHCompDriver.DEFAULT_FIND_QUESTION)))
	val FIND_TITLE = new RecombinationParameter[String]("findTitle", Some(List(FFVDefaultHCompDriver.DEFAULT_FIND_TITLE)))
	val FIX_TITLE = new RecombinationParameter[String]("fixTitle", Some(List(FFVDefaultHCompDriver.DEFAULT_FIX_TITLE)))
	val FIX_QUESTION = new RecombinationParameter[HCompInstructionsWithData]("fixQuestion", Some(List(FFVDefaultHCompDriver.DEFAULT_FIX_QUESTION)))
	val VERIFY_PROCESS = new RecombinationParameter[RecombinationStub[List[String], String]]("verifyProcess", Some(List(FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS)))

	val PATCHES_COUNT_IN_FIND = new RecombinationParameter[Integer]("patchesInFind", Some(List(10)))
	val FINDERS_COUNT = new RecombinationParameter[Integer]("findersCount", Some(List(3)))
	val MIN_FINDERS_TO_AGREE_FOR_FIX = new RecombinationParameter[Integer]("minFindersToAgree", Some(List(2)))
	val FIXERS_PER_PATCH = new RecombinationParameter[Integer]("fixersPerPatch", Some(List(3)))
}