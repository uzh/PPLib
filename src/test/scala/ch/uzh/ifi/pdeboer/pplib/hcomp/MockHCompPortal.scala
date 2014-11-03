package ch.uzh.ifi.pdeboer.pplib.hcomp

/**
 * Created by pdeboer on 29/10/14.
 */
class MockHCompPortal extends HCompPortalAdapter {
	var filters = List.empty[(HCompQuery) => Option[HCompAnswer]]

	override def getDefaultPortalKey: Symbol = 'testPortal

	def createMultipleChoiceFilterRule(trigger: String, selected: Set[String]) {
		val f = (q: HCompQuery) => {
			if (q.question.contains(trigger)) {
				val query: MultipleChoiceQuery = q.asInstanceOf[MultipleChoiceQuery]
				Some(MultipleChoiceAnswer(query,
					query.options.map(o => (o, selected.contains(o))).toMap
				))
			} else None
		}
		filters ::= f
	}

	def createFreeTextFilterRule(trigger: String, answer: String): Unit = {
		val f = (q: HCompQuery) => {
			if (q.question.contains(trigger)) {
				val query: FreetextQuery = q.asInstanceOf[FreetextQuery]
				Some(FreetextAnswer(query, answer))
			} else None
		}
		filters ::= f
	}

	override protected def processQuery(query: HCompQuery, props: HCompQueryProperties = HCompQueryProperties()): Option[HCompAnswer] = {
		query match {
			case composite: CompositeQuery =>
				Some(CompositeQueryAnswer(composite, composite.queries.map(q => (q, processQuery(q))).toMap))
			case _ =>
				filters.find(f => f(query).isDefined).get.apply(query)
		}
	}
}
