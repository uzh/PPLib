package ch.uzh.ifi.pdeboer.pplib.examples.boa

import ch.uzh.ifi.pdeboer.pplib.hcomp._

import scala.util.Random

/**
  * Created by pdeboer on 21/03/16.
  */
class BTSTestPortal(val probabilityForCapitalToBeSelected: Double = 0.7) extends HCompPortalAdapter {
	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		query match {
			case c: CompositeQuery => Some(CompositeQueryAnswer(c, c.queries.map(q => q -> processSubquery(q, properties)).toMap))
			case _ => processSubquery(query, properties)
		}
	}

	def processSubquery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		val targetState = BTSResult.stateToCities.keys.find(state => query.question.contains(state)).get
		if (query.question.contains("asked")) {
			//if other crowd workers were asked the same question..
			val targetCity = BTSResult.stateToCities(targetState).find(city => query.question.contains(city)).get
			val probaDouble = if (targetCity == BTSResult.groundTruth(targetState)) probabilityForCapitalToBeSelected else probaForNonCapital(targetState)
			val q = query.asInstanceOf[FreetextQuery]
			Some(FreetextAnswer(q, (probaDouble * 100).toInt.toString))
		} else if (query.question.contains("capital")) {
			//what is the capital of..
			val answer = randomlySelectAnswerFor(targetState)
			val mc = query.asInstanceOf[MultipleChoiceQuery]
			Some(MultipleChoiceAnswer(mc, mc.options.map(o => (o, answer == o)).toMap))
		} else None
	}

	def randomlySelectAnswerFor(state: String) = {
		val cities = BTSResult.stateToCities(state).sortBy(s => Random.nextDouble())
		val randomlySortedOptions = cities.map(c => (c, if (BTSResult.groundTruth(state) == c) probabilityForCapitalToBeSelected else probaForNonCapital(state)))

		val cdfOfOptions = randomlySortedOptions.zipWithIndex.map(t => randomlySortedOptions.take(t._2).map(_._2).sum + t._1._2)
		val targetRandom = Random.nextDouble()
		val firstLargerElement = cdfOfOptions.zipWithIndex.find(i => i._1 > targetRandom)
		val indexOfTargetElement = if (firstLargerElement.isDefined) firstLargerElement.get._2 else cdfOfOptions.length - 1

		randomlySortedOptions(indexOfTargetElement)._1
	}

	def probaForNonCapital(state: String) = (1.0d - probabilityForCapitalToBeSelected) / (BTSResult.stateToCities(state).size - 1)

	override def cancelQuery(query: HCompQuery): Unit = {}
}
