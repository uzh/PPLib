package ch.uzh.ifi.pdeboer.pplib.hcomp

/**
 * Created by pdeboer on 29/10/14.
 */
class MockHCompPortal extends HCompPortalAdapter {
	var filters = List.empty[(HCompQuery) => Option[HCompAnswer]]

	override def getDefaultPortalKey: String = "testPortal"

	override protected def processQuery(query: HCompQuery): Option[HCompAnswer] = {
		query match {
			case composite: CompositeQuery =>
				Some(CompositeQueryAnswer(composite, composite.queries.map(q => (q, processQuery(q))).toMap))
			case _ =>
				filters.find(f => f(query).isDefined).get.apply(query)
		}
	}
}
