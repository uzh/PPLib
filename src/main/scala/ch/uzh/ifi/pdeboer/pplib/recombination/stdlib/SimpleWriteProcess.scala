package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination.{ProcessParameter, ProcessStubWithHCompPortalAccess, PPLibProcess, ProcessStub}

/**
 * Created by pdeboer on 04/11/14.
 */
@PPLibProcess("create.collection.text")
class SimpleWriteProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[String], List[Map[String, String]]](params) {

	import SimpleWriteProcess._

	override protected def run(data: List[String]): List[Map[String, String]] = {
		val questionPerCriterion = getParamUnsafe(QUESTION_PER_CRITERION)
		getCrowdWorkers(getParamUnsafe(STORIES_PER_CRITERION)).map(i => {
			val queries = data.map(d => FreetextQuery(questionPerCriterion.getInstructions(d)) -> d).toMap

			val results = portal.sendQueryAndAwaitResult(
				CompositeQuery(queries.keys.toList, getParamUnsafe(INSTRUCTIONS))).get.as[CompositeQueryAnswer]

			queries.map {
				case (key, value) => value -> results.get[FreetextAnswer](key).answer
			}.toMap
		}).toList
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(QUESTION_PER_CRITERION, INSTRUCTIONS, STORIES_PER_CRITERION)
}

object SimpleWriteProcess {
	val QUESTION_PER_CRITERION = new ProcessParameter[HCompInstructionsWithTuple]("question_criterion", Some(List(HCompInstructionsWithTuple("Please answer the following question"))))
	val INSTRUCTIONS = new ProcessParameter[String]("instructions", Some(List("Please answer the following questions truthfully")))
	val STORIES_PER_CRITERION = new ProcessParameter[Int]("stories_count", Some(List(3, 5)))
}
