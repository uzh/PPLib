package ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal

import ch.uzh.ifi.pdeboer.pplib.hcomp._

import scala.util.Random

/**
 * Created by pdeboer on 07/01/15.
 */
@HCompPortal(builder = classOf[RandomPortalBuilder], autoInit = true)
class RandomHCompPortal(val param: String) extends HCompPortalAdapter {
	var answerPool: List[String] = Nil

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		query match {
			case x: FreetextQuery =>
				val answer: String = if (answerPool.size == 0) query.hashCode() + ""
				else {
					answerPool(Math.abs(answerPool.length * Random.nextDouble()).toInt)
				}
				Some(FreetextAnswer(x, answer))
			case x: MultipleChoiceQuery => {
				val upperBound = if (x.maxNumberOfResults < 1) x.options.length else x.maxNumberOfResults
				val numberToSelect = Random.nextDouble() * (upperBound - x.minNumberOfResults)
				val selections = x.options.sortBy(s => Random.nextDouble()).take(Math.max(1, numberToSelect.toInt))
				Some(MultipleChoiceAnswer(x, x.options.map(o => (o, selections.contains(o))).toMap))
			}
			case x: CompositeQuery => Some(CompositeQueryAnswer(x, x.queries.map(q => (q, processQuery(q, properties))).toMap))
		}
	}

	override def getDefaultPortalKey: String = RandomHCompPortal.PORTAL_KEY

	override def cancelQuery(query: HCompQuery): Unit = {}
}

object RandomHCompPortal {
	val PORTAL_KEY = "randomPortal"
	val CONFIG_PARAM = "hcomp.randomPortal.param"
}

class RandomPortalBuilder extends HCompPortalBuilder {
	val PARAM_KEY = "param"

	override val parameterToConfigPath = Map(PARAM_KEY -> RandomHCompPortal.CONFIG_PARAM)

	override def build: HCompPortalAdapter = new RandomHCompPortal(params(PARAM_KEY))

	override def expectedParameters: List[String] = List(PARAM_KEY)
}
