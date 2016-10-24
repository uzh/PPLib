package ch.uzh.ifi.pdeboer.pplib.examples.maintenance

import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import org.joda.time.DateTime

/**
  * Created by pdeboer on 24.10.16.
  */
object CloseAllHITS extends App {

	import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

	HComp.mechanicalTurk.service.SearchHITs().toList.filter(_.Expiration.isAfter(DateTime.now())).mpar.foreach(h => {
		try {
			HComp.mechanicalTurk.service.DisableHIT(h.HITId)
			println(s"disabled ${h.HITId}")
		} catch {
			case e: Throwable => println(s"couldn't disable hit ${h.HITId} $e")
		}
	})
}
