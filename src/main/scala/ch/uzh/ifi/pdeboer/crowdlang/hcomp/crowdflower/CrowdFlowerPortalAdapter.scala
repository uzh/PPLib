package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.crowdlang.hcomp._

/**
 * Created by pdeboer on 10/10/14.
 */
class CrowdFlowerPortalAdapter(applicationName: String, apiKey: String) extends HCompPortalAdapter {
	protected val worker = new CrowdFlowerWorker(applicationName, apiKey)

	protected override def processQuery(query: HCompQuery) = {
		query match {
			case x: FreetextQuery => worker.writeText(x)
			case x: MultipleChoiceQuery => worker.chooseOption(x)
			case _ => HCompException(query, new IllegalArgumentException("HComp method unknown"))
		}
	}
}
