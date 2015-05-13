package ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal

import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextAnswer, FreetextQuery}

import scala.util.Random

/**
 * Created by pdeboer on 13/05/15.
 */
class RandomHCompPortalWithDefinedCandidates(val query2AnswerCandidates: Map[String, List[String]]) extends RandomHCompPortal("") {
	override protected def processFreetextQuery(x: FreetextQuery): Option[FreetextAnswer] = {
		val candidateAnswers = query2AnswerCandidates.find(q2a => x.question.contains(q2a._1)).map(_._2)
		val randomlyPickedAnswer = candidateAnswers.map(a => a(Random.nextInt(a.size)))
		val fullAnswer = randomlyPickedAnswer.map(a => Some(FreetextAnswer(x, a)))

		fullAnswer.getOrElse(super.processFreetextQuery(x))
	}
}
