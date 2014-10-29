package ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower

import ch.uzh.ifi.pdeboer.crowdlang.hcomp._
import play.api.libs.json.{JsArray, JsValue}

/**
 * Created by pdeboer on 14/10/14.
 */
class CFFreetextQuery(val rawQuery: FreetextQuery, val name: String = "field") extends CFQuery {
	val xml = <cml:textarea name={name} label={rawQuery.question} default={rawQuery.defaultAnswer} class=" " instructions=" " validates="required"/>

	override def getCML(): String = xml.toString()

	override def interpretResult(json: JsValue): Option[FreetextAnswer] = {
		val result_field = json \\ name
		if (result_field != Nil) {
			val result = result_field.map(_.as[List[String]]).last.last
			Some(FreetextAnswer(rawQuery, result))
		} else None
	}
}

class CFMultipleChoiceQuery(val rawQuery: MultipleChoiceQuery, val fieldName: String = "field") extends CFQuery {
	val xml = <cml:checkboxes name={fieldName} label={rawQuery.question} class=" " instructions={rawQuery.question} validates="required">
		{rawQuery.options.map(o => <cml:checkbox label={o}/>)}
	</cml:checkboxes>

	override def getCML(): String = xml.toString()

	override def interpretResult(json: JsValue): Option[HCompAnswer] = {
		val resultField = json \\ fieldName
		if (resultField != Nil) {
			val result = (resultField(0) \ "res")(0).asInstanceOf[JsArray].value.map(s => s.toString.substring(1, s.toString().length - 1)).toSet
			val selectedElementsOutOfOriginalQuery = rawQuery.options.map(o => (o -> result.contains(o))).toMap
			Some(MultipleChoiceAnswer(rawQuery, selectedElementsOutOfOriginalQuery))
		} else None
	}
}

class CFSingleChoiceQuery(val rawQuery: MultipleChoiceQuery, val fieldName: String = "field") extends CFQuery {
	val xml = <cml:radios name={fieldName} label={rawQuery.question} class=" " instructions={rawQuery.question} validates="required">
		{rawQuery.options.map(o => <cml:radio label={o}/>)}
	</cml:radios>

	override def getCML(): String = xml.toString()

	override def interpretResult(json: JsValue): Option[HCompAnswer] = {
		val resultField = json \\ fieldName
		if (resultField != Nil) {
			val result = (resultField(0) \ "res").asInstanceOf[JsArray].value.map(s => s.toString.substring(1, s.toString().length - 1)).toSet
			val selectedElementsOutOfOriginalQuery = rawQuery.options.map(o => (o -> result.contains(o))).toMap
			Some(MultipleChoiceAnswer(rawQuery, selectedElementsOutOfOriginalQuery))
		} else None
	}
}

class CFCompositeQuery(val rawQuery: CompositeQuery) extends CFQuery {
	val children = rawQuery.queries.zipWithIndex.map((t) => CFConversions.convertQueryToCFQuery(t._1, "field" + t._2)).toList

	override def getCML(): String = children.map(_.getCML()).mkString("<p>  </p>")

	override def interpretResult(json: JsValue): Option[HCompAnswer] = {
		val answers = children.map(c => (c.rawQuery, c.interpretResult(json))).toMap
		if (answers.values.exists(_.isDefined))
			Some(new CompositeQueryAnswer(rawQuery, answers)) //only return if defined
		else None
	}
}


object CFConversions {
	def convertQueryToCFQuery(q: HCompQuery, fieldName: String = "field"): CFQuery = q match {
		case q: FreetextQuery => new CFFreetextQuery(q, fieldName)
		case q: MultipleChoiceQuery => if (q.maxNumberOfResults == 1) new CFSingleChoiceQuery(q, fieldName) else new CFMultipleChoiceQuery(q, fieldName)
		case q: CompositeQuery => new CFCompositeQuery(q)
	}
}

trait CFQuery {
	def getCML(): String

	def rawQuery: HCompQuery

	def interpretResult(json: JsValue): Option[HCompAnswer]
}