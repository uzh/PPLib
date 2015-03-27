package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessParameter, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.{U, SimpleClassTag}

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 27/03/15.
 */
class Recombinator[INPUT_TYPE, OUTPUT_TYPE](db: RecombinationDB = RecombinationDB.DEFAULT)(implicit inputType: ClassTag[INPUT_TYPE], outputType: ClassTag[OUTPUT_TYPE]) {
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
			gen.generatePassableProcesses()
		}).toList.flatten
	}
}

trait RecombinationHint {
	def filter[T <: ProcessStub[_, _]](clazz: Class[T]): Boolean

	def constructionParameters: Map[String, Iterable[Any]]
}

class TypeRecombinationHint[T <: ProcessStub[_, _]]()(implicit baseClass: ClassTag[T]) extends RecombinationHint {
	val base = baseClass.runtimeClass

	override def filter[T <: ProcessStub[_, _]](candidateClass: Class[T]): Boolean = {
		val allowedClasses = U.getAncestorsOfClass(candidateClass)
		allowedClasses.contains(base)
	}

	override def constructionParameters: Map[String, Iterable[Any]] = Map.empty
}


class OptionalParameterRecombinationHint[T: ClassTag](val param: ProcessParameter[T], val values: Iterable[T]) extends RecombinationHint {
	override def filter[BASE <: ProcessStub[_, _]](candidateClass: Class[BASE]): Boolean = true

	override def constructionParameters: Map[String, Iterable[Any]] =
		Map(param.key -> values)
}