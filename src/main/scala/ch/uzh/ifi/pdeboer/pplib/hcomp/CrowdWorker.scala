package ch.uzh.ifi.pdeboer.pplib.hcomp

/**
 * Created by pdeboer on 26/11/14.
 */
//TODO implement me
class CrowdWorker(val id: String) extends HCompPortalAdapter {
	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = ???

	override def getDefaultPortalKey: String = ???

	override def cancelQuery(query: HCompQuery): Unit = ???
}
