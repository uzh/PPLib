package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower

import play.api.libs.json.JsValue

/**
 * Created by Marc Tobler on 30.07.2014.
 * adapted by pdeboer on 10/10/14.
 */
class CFSingleChoiceJob(jobId: Int, apiKey: String, fieldName: String = "agg") extends CrowdFlowerJob[String](jobId, apiKey) {

	override protected def extractResult(json: JsValue): Option[String] = {
		val agg_result = (json \\ fieldName)
		if (agg_result != Nil) {
			val result = agg_result.map(_.as[String]).last
			println(s"$jobId : Result fetched")
			Some(result)
		} else {
			println(s"$jobId : Found none")
			None
		}
		//TODO throw some exception if json is not correctly structure
	}
}
