package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{ProcessParameter, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 10/04/15.
 */
trait RecombinationHint {
	def filter[T <: ProcessStub[_, _]](clazz: Class[T]): Boolean

	def processConstructionParameter: Map[String, Iterable[Any]]
}

class TypeRecombinationHint[T <: ProcessStub[_, _]]()(implicit baseClass: ClassTag[T]) extends TypeUnsafeRecombinationHint(baseClass.runtimeClass.asInstanceOf[Class[ProcessStub[_, _]]]) {}

class TypeUnsafeRecombinationHint(base: Class[ProcessStub[_, _]]) extends RecombinationHint {
	override def filter[T <: ProcessStub[_, _]](candidateClass: Class[T]): Boolean = {
		val allowedClasses = U.getAncestorsOfClass(candidateClass)
		allowedClasses.contains(base)
	}

	override def processConstructionParameter: Map[String, Iterable[Any]] = Map.empty
}

class OptionalParameterRecombinationHint[T: ClassTag](val param: ProcessParameter[T], val values: Iterable[T]) extends RecombinationHint {
	override def filter[BASE <: ProcessStub[_, _]](candidateClass: Class[BASE]): Boolean = true

	override def processConstructionParameter: Map[String, Iterable[Any]] =
		Map(param.key -> values)
}

trait RecombinationHintGenerator {
	def createHints(): Iterable[RecombinationHint]
}

class RecombinationHintGroup(val classUnderRecombination: Option[Class[ProcessStub[_, _]]], val hints: List[RecombinationHint]) {}