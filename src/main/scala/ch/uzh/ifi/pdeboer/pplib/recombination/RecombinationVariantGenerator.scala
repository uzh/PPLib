package ch.uzh.ifi.pdeboer.pplib.recombination

import java.lang.reflect.Constructor

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Created by pdeboer on 09/10/14.
 */
class RecombinationVariantGenerator(configs: Map[String, List[ProcessStub[_, _]]]) {
	lazy val variants = {
		val listOfTupleLists: List[List[(String, ProcessStub[_, _])]] = configs.map(k => k._2.map(r => (k._1, r)).toList).toList
		CombinationGenerator.generate(listOfTupleLists).map(k => {
			RecombinationVariant(k.asInstanceOf[List[(String, ProcessStub[_, _])]].toMap)
		})
	}
}

trait RecombinationStubParameterVariantGenerator[T] {
	protected def targetConstructor: Constructor[_]

	protected def base: ProcessStub[_, _]


	protected var parameterValues = new mutable.HashMap[String, mutable.Set[Any]]()

	def initAllParamsWithCandidates() = {
		base.allParams.foreach(k => addParameterVariations(k.key, k.candidateDefinitions.getOrElse(Nil).toList.asInstanceOf[List[AnyRef]]))
		this
	}

	def addParameterVariations(paramKey: String, values: List[Any]) = {
		var cur = parameterValues.getOrElse(paramKey, mutable.HashSet.empty[Any])
		values.foreach(k => {
			if (base.isParameterTypeCorrect(paramKey, k))
				cur += k
			else throw new IllegalArgumentException("Parameter type incorrect for " + paramKey)
		})
		parameterValues += (paramKey -> cur)
		this
	}

	def generateParameterVariations(): List[Map[String, Any]] = {
		val listOfTupleLists = parameterValues.map(k => k._2.map(r => (k._1, r)).toList).toList
		CombinationGenerator.generate(listOfTupleLists).map(k => {
			k.asInstanceOf[List[(String, Any)]].toMap
		})
	}

	def generateVariationsAndInstanciate(): List[T] =
		generateParameterVariations()
			.map(params => targetConstructor.newInstance(params))
			.asInstanceOf[List[T]]
}

class InstanciatedRecombinationStubParameterVariantGenerator[T: ClassTag](_base: T, initWithDefaults: Boolean = false) extends RecombinationStubParameterVariantGenerator[T] {
	override protected def base: ProcessStub[_, _] = _base.asInstanceOf[ProcessStub[_, _]]

	override protected def targetConstructor: Constructor[_] = base.getClass.getConstructor(classOf[Map[String, Any]])

	if (initWithDefaults) initAllParamsWithCandidates()
}

class TypedRecombinationStubParameterVariantGenerator[T: ClassTag](initWithDefaults: Boolean = false) extends RecombinationStubParameterVariantGenerator[T] {
	//very ugly stuff //TODO check
	val declaredConstructors = implicitly[ClassTag[T]].runtimeClass.getDeclaredConstructors
	protected val targetConstructor: Constructor[_] = implicitly[ClassTag[T]].runtimeClass.getDeclaredConstructor(classOf[Map[String, Any]])
	protected val base: ProcessStub[_, _] = targetConstructor.newInstance(Map.empty[String, Any]).asInstanceOf[ProcessStub[_, _]]

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