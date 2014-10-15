package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.crowdlang.hcomp._
import play.api.libs.json.JsValue

/**
 * Created by pdeboer on 14/10/14.
 */
class CFFreetextQuery(val rawQuery: FreetextQuery, val name: String = "freetextQuery") extends CFQuery {
	val xml = <cml:textarea name={name} label={rawQuery.question} class=" " instructions=" " default=" " validates="required"/>

	override def getCML(): String = xml.toString()

	override def interpretResult(json: JsValue): Option[FreetextAnswer] = {
		val result_field = json \\ name
		if (result_field != Nil) {
			val result = result_field.map(_.as[List[String]]).last.last
			Some(FreetextAnswer(rawQuery, result))
		} else None
	}
}

class CFMultipleChoiceQuery(val rawQuery: MultipleChoiceQuery, val fieldName: String = "multipleChoice") extends CFQuery {
	val xml = <cml:checkboxes name={fieldName} label={rawQuery.question} class=" " instructions={rawQuery.question} validates="required">
		{rawQuery.options.map(o => <cml:checkbox label={o}/>)}
	</cml:checkboxes>

	override def getCML(): String = xml.toString()

	override def interpretResult(json: JsValue): Option[HCompAnswer] = {
		val result_field = json \\ fieldName
		if (result_field != Nil) {
			val result = result_field.map(_.as[String]).toSet
			val selectedElementsOutOfOriginalQuery = rawQuery.options.map(o => (o -> result.contains(o))).toMap
			Some(MultipleChoiceAnswer(rawQuery, selectedElementsOutOfOriginalQuery))
		} else None
	}
}

class CFSingleChoiceQuery(val rawQuery: MultipleChoiceQuery, val fieldName: String = "singleChoice") extends CFQuery {
	val xml = <cml:radios name={fieldName} label={rawQuery.question} class=" " instructions={rawQuery.question} validates="required">
		{rawQuery.options.map(o => <cml:radio label={o}/>)}
	</cml:radios>

	override def getCML(): String = xml.toString()

	override def interpretResult(json: JsValue): Option[HCompAnswer] = {
		val result_field = json \\ fieldName
		if (result_field != Nil) {
			val result = result_field.map(_.as[String]).toSet
			val selectedElementsOutOfOriginalQuery = rawQuery.options.map(o => (o -> result.contains(o))).toMap
			Some(MultipleChoiceAnswer(rawQuery, selectedElementsOutOfOriginalQuery))
		} else None
	}
}

object CFConversions {
	implicit def freeTextQueryToCFQuery(q: FreetextQuery) = new CFFreetextQuery(q)

	implicit def multipleChoiceQueryToCFQuery(q: MultipleChoiceQuery) =
		if (q.maxNumberOfResults == 1) new CFSingleChoiceQuery(q) else new CFMultipleChoiceQuery(q)
}

trait CFQuery {
	def getCML(): String

	def rawQuery: HCompQuery

	def interpretResult(json: JsValue): Option[HCompAnswer]
}