package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{ProcessParameter, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Created by pdeboer on 10/04/15.
 */
trait RecombinationHint {
	def processConstructionParameter: Map[String, Iterable[Any]] = Map.empty

	def runComplexParameterRecombinationOn(processParameter: String): Option[Boolean] = None

	def addDefaultValuesFromParamDefinition(processParameter: String): Option[Boolean] = None
}

class AddedParameterRecombinationHint[T: ClassTag](val param: ProcessParameter[T], val values: Iterable[T]) extends RecombinationHint {
	override def processConstructionParameter: Map[String, Iterable[Any]] =
		Map(param.key -> values)
}

class SettingsOnParamsRecombinationHint(val targetParams: List[String] = Nil, val runComplexParameterRecombinationOnThisParam: Option[Boolean] = Some(true), val addDefaultValuesForParam: Option[Boolean] = Some(true)) extends RecombinationHint {
	override def runComplexParameterRecombinationOn(processParameterKey: String): Option[Boolean] = if (IsParamTargetParam(processParameterKey)) runComplexParameterRecombinationOnThisParam else None

	override def addDefaultValuesFromParamDefinition(processParameterKey: String): Option[Boolean] = if (IsParamTargetParam(processParameterKey)) addDefaultValuesForParam else None

	private def IsParamTargetParam(processParameterKey: String): Boolean = {
		targetParams.size == 0 || targetParams.contains(processParameterKey)
	}
}

class RecombinationHints(val hints: Map[Option[Class[ProcessStub[_, _]]], List[RecombinationHint]] = Map.empty) {
	/**
	 * convert hints map of classes to types such that we don't lose the information stored in generics due to type erasure
	 */
	private val _hints = hints.map(h => {
		val key = h._1.map(c => U.getTypeFromClass(c))
		(key, h._2)
	})


	def +=(hint: List[RecombinationHint]): RecombinationHints =
		addHint(hint, null)

	def +=(hint: RecombinationHint): RecombinationHints =
		addHint(List(hint), null)

	def addHint[BASE <: ProcessStub[_, _]](hint: List[RecombinationHint], targetProcess: Class[BASE] = null) = {
		val targetProcessOption = Option(targetProcess).asInstanceOf[Option[Class[ProcessStub[_, _]]]]
		val newHintLine = hint ::: hints.getOrElse(targetProcessOption, List.empty[RecombinationHint])
		new RecombinationHints(hints + (targetProcessOption -> newHintLine))
	}

	def apply[BASE <: ProcessStub[_, _]](clazz: Class[BASE] = null) = {
		hintsForType(U.getTypeFromClass(clazz))
	}

	def hintsForType(t: Type = null) = {
		defaultHints ::: (if (t == null) Nil else _hints.getOrElse(Option(t), Nil))
	}

	def defaultHints: List[RecombinationHint] = {
		_hints.getOrElse(None, Nil)
	}

	def singleValueHint[T](t: Type = null, paramKey: String, function: (RecombinationHint, String) => Option[T]): Option[T] = {
		val allPossibleValues: List[Option[T]] = hintsForType(t).map(h => function(h, paramKey))
		allPossibleValues.foldLeft(allPossibleValues.headOption.getOrElse(None))((prev, newItem) => newItem)
	}
}

object RecombinationHints {
	def create(hints: Map[Class[_ <: ProcessStub[_, _]], List[_ <: RecombinationHint]]) = {
		val hintsToUse = hints.map(h => {
			val key = if (h._1 == classOf[DefaultHintProcessStub]) None else Some(h._1)
			(key.asInstanceOf[Option[Class[ProcessStub[_, _]]]], h._2)
		})
		new RecombinationHints(hintsToUse)
	}

	val DEFAULT_HINTS = classOf[DefaultHintProcessStub]

	class DefaultHintProcessStub private[RecombinationHints]() extends ProcessStub[String, String](Map.empty) {
		override protected def run(data: String): String = data
	}

}