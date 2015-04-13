package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.SimpleClassTag

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 27/03/15.
 */
class Recombinator[INPUT_TYPE, OUTPUT_TYPE](_hints: List[RecombinationHint] = List.empty[RecombinationHint], db: RecombinationDB = RecombinationDB.DEFAULT)(implicit inputType: ClassTag[INPUT_TYPE], outputType: ClassTag[OUTPUT_TYPE]) {
	private var hints = List.empty[RecombinationHint]

	def addHint(hint: RecombinationHint) = {
		hints ::= hint
		this
	}

	def materialize: List[PassableProcessParam[ProcessStub[INPUT_TYPE, OUTPUT_TYPE]]] = {
		val targetClasses = db.classes.filter(t => hints.forall(c => c.filter(t)))
		targetClasses.map(cls => {
			val gen = new TypedParameterVariantGenerator[ProcessStub[INPUT_TYPE, OUTPUT_TYPE]]()(new SimpleClassTag[INPUT_TYPE, OUTPUT_TYPE](cls))
			hints.foreach(hint => {
				hint.constructionParameters.foreach {
					case (parameterKey, parameterValue) => gen.addParameterVariations(parameterKey, parameterValue)
				}
			})
			gen.uncoveredParameterThatAreExpected.foreach(p => {
				//TODO find all applicable classes using some strategy/resolver and add it as passable
			})

			gen.generatePassableProcesses()
		}).toList.flatten
	}
}