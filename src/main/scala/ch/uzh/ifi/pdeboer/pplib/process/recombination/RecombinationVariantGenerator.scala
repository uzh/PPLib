package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessParameter, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.collection.mutable
import scala.reflect.api.Universe
import scala.reflect.runtime.universe._
import scala.reflect.{ClassTag, api}


/**
 * Created by pdeboer on 09/10/14.
 */
class RecombinationVariantGenerator(configs: Map[String, List[PassableProcessParam[_ <: ProcessStub[_, _]]]]) {
	lazy val variants = {
		val listOfTupleLists: List[List[(String, PassableProcessParam[_ <: ProcessStub[_, _]])]] = configs.map(k => k._2.map(r => (k._1, r)).toList).toList
		CombinationGenerator.generate(listOfTupleLists).map(k => {
			new RecombinedProcessBlueprint(k.toMap)
		})
	}
}

abstract class ParameterVariantGenerator[T <: ProcessStub[_, _]]() {
	protected def base: ProcessStub[_, _]

	def baseClassTag: ClassTag[T]

	def baseTypeTag: TypeTag[T]

	protected var parameterValues = new mutable.HashMap[String, mutable.Set[Any]]()

	def initAllParamsWithCandidates() = {
		base.allParams.foreach(k => addParameterVariations(k.key, k.candidateDefinitions.getOrElse(Nil).toList.asInstanceOf[List[AnyRef]]))
		this
	}

	def addParameterVariations(paramKey: String, values: Iterable[Any]) = {
		var cur = parameterValues.getOrElse(paramKey, mutable.HashSet.empty[Any])
		values.foreach(k => {
			//TODO find problem
			//if (base.isParameterTypeCorrect(paramKey, k))
			cur += k
			/*
			else {
				throw new IllegalArgumentException("Parameter type incorrect for " + paramKey)
			 }*/
		})
		parameterValues += (paramKey -> cur)
		this
	}

	def addVariation(param: ProcessParameter[_], values: Iterable[Any]) =
		addParameterVariations(param.key, values)

	def generateParameterVariations(): List[Map[String, Any]] = {
		val listOfTupleLists = parameterValues.map(k => k._2.map(r => (k._1, r)).toList).toList
		CombinationGenerator.generate(listOfTupleLists).map(_.toMap)
	}

	def generatePassableProcesses(): List[PassableProcessParam[T]] =
		generateParameterVariations().map(params => {
			new PassableProcessParam[T](params)(baseClassTag, baseTypeTag)
		})

	def uncoveredParameterThatAreExpected: Set[ProcessParameter[_]] = {
		val expected: List[ProcessParameter[_]] = base.expectedParametersBeforeRun ::: base.expectedParametersOnConstruction
		val keysOfMissingParameters = expected.map(_.key).toSet.diff(parameterValues.map(_._1).toSet)
		val expectedParameterAsDictionary: Map[String, ProcessParameter[_]] = expected.map(e => (e.key, e)).toMap
		keysOfMissingParameters.map(k => expectedParameterAsDictionary(k))
	}

	def generateVariationsAndInstanciate(): List[T] =
		generateParameterVariations()
			.map(params => ProcessStub.create[T](params)(baseClassTag))
			.asInstanceOf[List[T]]
}

class InstanciatedParameterVariantGenerator[T <: ProcessStub[_, _]](_base: T, initWithDefaults: Boolean = false, _baseClass: Class[T] = null, _baseType: Type = null) extends ParameterVariantGenerator[T] {
	override protected def base: ProcessStub[_, _] = _base.asInstanceOf[ProcessStub[_, _]]

	override val baseClassTag: ClassTag[T] = ClassTag(Option(_baseClass).getOrElse(base.getClass))

	class MyTypeTag[TPE] private[InstanciatedParameterVariantGenerator](clazz: Class[_], val tpe: Type) extends TypeTag[TPE] {
		override val mirror = runtimeMirror(clazz.getClassLoader)

		override def in[U <: Universe with Singleton](otherMirror: api.Mirror[U]): U#TypeTag[TPE] = throw new IllegalAccessException("this shouldn't happen")
	}

	override val baseTypeTag = new MyTypeTag[T](baseClassTag.runtimeClass, Option(_baseType).getOrElse(U.getTypeFromClass(baseClassTag.runtimeClass)))

	if (initWithDefaults) initAllParamsWithCandidates()
}

class TypedParameterVariantGenerator[T <: ProcessStub[_, _]](initWithDefaults: Boolean = false)(implicit val baseClassTag: ClassTag[T], val baseTypeTag: TypeTag[T]) extends ParameterVariantGenerator[T] {
	protected val base = ProcessStub.create[T](Map.empty[String, Any])
	if (initWithDefaults) initAllParamsWithCandidates()
}

object CombinationGenerator {
	/**
	 * taken from http://stackoverflow.com/questions/23425930/generating-all-possible-combinations-from-a-listlistint-in-scala
	 * and adapted
	 * @param x
	 * @return
	 */
	def generate[T](x: List[List[T]]): List[List[T]] = {
		val listWithoutEmptyLists = x.filter(_.length > 0)
		listWithoutEmptyLists match {
			case Nil => List(Nil)
			case h :: t => for (j <- generate(t); i <- h) yield i :: j
		}
	}
}