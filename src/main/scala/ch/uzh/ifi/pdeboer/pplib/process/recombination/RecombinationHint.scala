package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{ProcessParameter, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 10/04/15.
 */
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

trait RecombinationHintGenerator {
	def createHints(): Iterable[RecombinationHint]
}