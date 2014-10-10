package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.crowdlang.hcomp._

import scala.concurrent.Future

/**
 * Created by pdeboer on 10/10/14.
 */
class CrowdFlowerPortalAdapter(apiKey: String) extends HCompPortalAdapter {
	protected val worker = new CrowdFlowerWorker("CrowdLang", apiKey)

	override def sendQuery(query: HCompQuery): Future[HCompAnswer] = {
		query match {
			case x: FreetextQuery => worker.writeText(x)
			case x: MultipleChoiceQuery => worker.chooseOption(x)
			case _ => Future[HCompException] {
				HCompException(query, new IllegalArgumentException("HComp method unknown"))
			}
		}
	}
}
