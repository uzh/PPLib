package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.process.entities._
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

	def addDefaultValuesFromProcessDefinition(processParameter: String): Option[Boolean] = None
}

class AddedParameterRecombinationHint[T: ClassTag](val param: ProcessParameter[T], val values: Iterable[T]) extends RecombinationHint {
	override def processConstructionParameter: Map[String, Iterable[Any]] =
		Map(param.key -> values)


	override def toString = s"AddedParameterRecombinationHint($param, $values)"
}

class SettingsOnParamsRecombinationHint(val targetParams: List[String] = Nil, val runComplexParameterRecombinationOnThisParam: Option[Boolean] = Some(true), val addGeneralDefaultValuesForParam: Option[Boolean] = Some(true), val addLocalDefaultValuesForParam: Option[Boolean] = Some(true)) extends RecombinationHint {
	override def runComplexParameterRecombinationOn(processParameterKey: String): Option[Boolean] = if (isParamTargetParam(processParameterKey)) runComplexParameterRecombinationOnThisParam else None

	override def addDefaultValuesFromParamDefinition(processParameterKey: String): Option[Boolean] = if (isParamTargetParam(processParameterKey)) addGeneralDefaultValuesForParam else None

	override def addDefaultValuesFromProcessDefinition(processParameterKey: String): Option[Boolean] = if (isParamTargetParam(processParameterKey)) addLocalDefaultValuesForParam else None

	private def isParamTargetParam(processParameterKey: String): Boolean = {
		targetParams.isEmpty || targetParams.contains(processParameterKey)
	}


	override def toString = s"SettingsOnParamsRecombinationHint($targetParams, $runComplexParameterRecombinationOnThisParam, $addGeneralDefaultValuesForParam)"
}

class RecombinationHints(val hints: Map[Option[Class[ProcessStub[_, _]]], List[RecombinationHint]] = Map.empty) {
	/**
	  * convert hints map of classes to types such that we don't lose the information stored in generics due to type erasure
	  */
	private val _hints = hints.map(h => {
		val key = h._1.map(c => U.getTypeFromClass(c))
		(key, h._2)
	})

	override def toString = {
		_hints.map {
			case (targetClass, hintsList) => targetClass + "-> [" + hintsList.mkString(",") + "]"
		}.mkString(" || ")

	}

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
		allPossibleValues.foldLeft(allPossibleValues.headOption.getOrElse(None))((prev, newItem) => if (newItem.isDefined) newItem else prev)
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

	def hcompPlatform(l: List[HCompPortalAdapter]): List[RecombinationHint] = List(new SettingsOnParamsRecombinationHint(List(DefaultParameters.PORTAL_PARAMETER.key), addGeneralDefaultValuesForParam = Some(false)), new AddedParameterRecombinationHint[HCompPortalAdapter](DefaultParameters.PORTAL_PARAMETER, l))

	def instructions(l: List[InstructionData]): List[RecombinationHint] = List(new SettingsOnParamsRecombinationHint(List(DefaultParameters.INSTRUCTIONS.key), addGeneralDefaultValuesForParam = Some(false)), new AddedParameterRecombinationHint[InstructionData](DefaultParameters.INSTRUCTIONS, l))

	def instructionPool(p: Map[_root_.scala.reflect.runtime.universe.Type, InstructionGenerator]) = {
		List(new AddedParameterRecombinationHint[Map[_root_.scala.reflect.runtime.universe.Type, InstructionGenerator]](DefaultParameters.INSTRUCTION_GENERATOR_POOL, List(p)))
	}

	class DefaultHintProcessStub private[RecombinationHints]() extends ProcessStub[String, String](Map.empty) {
		override protected def run(data: String): String = data
	}

}