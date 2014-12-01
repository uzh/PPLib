package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination._

/**
 * Created by pdeboer on 04/11/14.
 */
@PPLibProcess("create.collection.text")
class SimpleWriteProcess(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStubWithHCompPortalAccess[List[String], List[Map[String, String]]](params) {
	import SimpleWriteProcess._

	override protected def run(data: List[String]): List[Map[String, String]] = {
		val questionPerCriterion = QUESTION_PER_CRITERION.get
		getCrowdWorkers(STORIES_PER_CRITERION.get).map(i => {
			val queries = data.map(d => FreetextQuery(questionPerCriterion.getInstructions(d)) -> d).toMap

			val results = portal.sendQueryAndAwaitResult(
				CompositeQuery(queries.keys.toList, INSTRUCTIONS.get)).get.as[CompositeQueryAnswer]

			queries.map {
				case (key, value) => value -> results.get[FreetextAnswer](key).answer
			}.toMap
		}).toList
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		List(QUESTION_PER_CRITERION, INSTRUCTIONS, STORIES_PER_CRITERION)
}

object SimpleWriteProcess {
	val QUESTION_PER_CRITERION = new ProcessParameter[HCompInstructionsWithTuple]("criterion", QuestionParam(), Some(List(HCompInstructionsWithTuple("Please answer the following question"))))
	val INSTRUCTIONS = new ProcessParameter[String]("instructions", QuestionParam(), Some(List("Please answer the following questions truthfully")))
	val STORIES_PER_CRITERION = new ProcessParameter[Int]("stories_count", OtherParam(), Some(List(3, 5)))
}
