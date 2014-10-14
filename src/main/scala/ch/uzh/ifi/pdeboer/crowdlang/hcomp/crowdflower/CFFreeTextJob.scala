package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower

import play.api.libs.json.JsValue

/**
 * Created by Marc Tobler on 30.07.2014.
 * adapted by pdeboer on 10/10/14.
 */
class CFFreeTextJob(jobId: Int, apiKey: String, fieldName: String = "response") extends CrowdFlowerJob[String](jobId, apiKey) {

	override protected def extractResult(json: JsValue): Option[String] = {
		val result_field = (json \\ fieldName)
		if (result_field != Nil) {
			val result = result_field.map(_.as[List[String]]).last.last
			println(s"$jobId : Result fetched")
			Some(result)
		} else {
			println(s"$jobId : Found none")
			None
		}
		//TODO throw some exception if json is not correctly structure
	}
}
