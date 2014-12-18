package ch.uzh.ifi.pdeboer.pplib.process.recombination

import java.lang.reflect.Constructor

import ch.uzh.ifi.pdeboer.pplib.process.entities.PassableProcessParam
import ch.uzh.ifi.pdeboer.pplib.process.{ProcessParameter, ProcessStub}

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Created by pdeboer on 09/10/14.
 */
class RecombinationVariantGenerator(configs: Map[String, List[PassableProcessParam[_, _]]]) {
	lazy val variants = {
		val listOfTupleLists: List[List[(String, PassableProcessParam[_, _])]] = configs.map(k => k._2.map(r => (k._1, r)).toList).toList
		CombinationGenerator.generate(listOfTupleLists).map(k => {
			new RecombinationVariant(k.asInstanceOf[List[(String, PassableProcessParam[_, _])]].toMap)
		})
	}
}

abstract class ParameterVariantGenerator[T: ClassTag] {
	protected def targetConstructor: Constructor[_]

	protected def base: ProcessStub[_, _]

	protected var parameterValues = new mutable.HashMap[String, mutable.Set[Any]]()

	def initAllParamsWithCandidates() = {
		base.allParams.foreach(k => addParameterVariations(k.key, k.candidateDefinitions.getOrElse(Nil).toList.asInstanceOf[List[AnyRef]]))
		this
	}

	def addParameterVariations(paramKey: String, values: Iterable[Any]) = {
		var cur = parameterValues.getOrElse(paramKey, mutable.HashSet.empty[Any])
		values.foreach(k => {
			if (base.isParameterTypeCorrect(paramKey, k))
				cur += k
			else {
				throw new IllegalArgumentException("Parameter type incorrect for " + paramKey)
			}
		})
		parameterValues += (paramKey -> cur)
		this
	}

	def addVariation(param: ProcessParameter[_], values: Iterable[Any]) =
		addParameterVariations(param.key, values)

	def generateParameterVariations(): List[Map[String, Any]] = {
		val listOfTupleLists = parameterValues.map(k => k._2.map(r => (k._1, r)).toList).toList
		CombinationGenerator.generate(listOfTupleLists).map(k => {
			k.asInstanceOf[List[(String, Any)]].toMap
		})
	}

	def generatePassableProcesses[IN: ClassTag, OUT: ClassTag](): List[PassableProcessParam[_, _]] =
		generateParameterVariations().map(params => {
			new PassableProcessParam[IN, OUT](base.asInstanceOf[ProcessStub[IN, OUT]].getClass, params)
		})

	def generateVariationsAndInstanciate(): List[T] =
		generateParameterVariations()
			.map(params => ProcessStub.typelessCreate(base.getClass, params))
			.asInstanceOf[List[T]]
}

class InstanciatedParameterVariantGenerator[T: ClassTag](_base: T, initWithDefaults: Boolean = false) extends ParameterVariantGenerator[T] {
	override protected def base: ProcessStub[_, _] = _base.asInstanceOf[ProcessStub[_, _]]

	override protected def targetConstructor: Constructor[_] = base.getClass.getConstructor(classOf[Map[String, Any]])

	if (initWithDefaults) initAllParamsWithCandidates()
}

class TypedParameterVariantGenerator[T: ClassTag](initWithDefaults: Boolean = false) extends ParameterVariantGenerator[T] {
	val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[ProcessStub[_, _]]]
	val declaredConstructors = clazz.getDeclaredConstructors
	protected val targetConstructor: Constructor[_] = clazz.getDeclaredConstructor(classOf[Map[String, Any]])
	protected val base: ProcessStub[_, _] = ProcessStub.typelessCreate(clazz, Map.empty) //TODO change me to typeful creation

	if (initWithDefaults) initAllParamsWithCandidates()
}

object CombinationGenerator {
	/**
	 * taken from http://stackoverflow.com/questions/23425930/generating-all-possible-combinations-from-a-listlistint-in-scala
	 * and adapted
	 * @param x
	 * @return
	 */
	def generate(x: List[List[AnyRef]]): List[List[AnyRef]] =
		x match {
			case Nil => List(Nil)
			case h :: t => for (j <- generate(t); i <- h) yield i :: j
		}
}