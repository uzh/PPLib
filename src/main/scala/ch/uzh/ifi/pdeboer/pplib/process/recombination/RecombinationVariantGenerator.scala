package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessParameter, ProcessStub}

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Created by pdeboer on 09/10/14.
 */
class RecombinationVariantGenerator(configs: Map[String, List[PassableProcessParam[_ <: ProcessStub[_, _]]]]) {
	lazy val variants = {
		val listOfTupleLists: List[List[(String, PassableProcessParam[_ <: ProcessStub[_, _]])]] = configs.map(k => k._2.map(r => (k._1, r)).toList).toList
		CombinationGenerator.generate(listOfTupleLists).map(k => {
			new RecombinationVariant(k.asInstanceOf[List[(String, PassableProcessParam[_ <: ProcessStub[_, _]])]].toMap)
		})
	}
}

abstract class ParameterVariantGenerator[T <: ProcessStub[_, _]]()(implicit baseCls: ClassTag[T]) {
	protected def base: ProcessStub[_, _]

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
		CombinationGenerator.generate(listOfTupleLists).map(k => {
			k.asInstanceOf[List[(String, Any)]].toMap
		})
	}

	def generatePassableProcesses(): List[PassableProcessParam[T]] =
		generateParameterVariations().map(params => {
			new PassableProcessParam[T](params)
		})

	def uncoveredParameterThatAreExpected = {
		val expected = base.expectedParametersBeforeRun ::: base.expectedParametersOnConstruction
		val keysOfMissingParameters = expected.map(_.key).toSet.diff(parameterValues.map(_._1).toSet)
		//val expectedParameterAsDictionary = expected.map(e => (e.key -> e)).toMap
		//keysOfMissingParameters.map(k => expectedParameterAsDictionary(k))
		Nil
	}

	def generateVariationsAndInstanciate(): List[T] =
		generateParameterVariations()
			.map(params => ProcessStub.create[T](params))
			.asInstanceOf[List[T]]
}

class InstanciatedParameterVariantGenerator[T <: ProcessStub[_, _]](_base: T, initWithDefaults: Boolean = false)(implicit baseClass: ClassTag[T]) extends ParameterVariantGenerator[T] {
	override protected def base: ProcessStub[_, _] = _base.asInstanceOf[ProcessStub[_, _]]

	if (initWithDefaults) initAllParamsWithCandidates()
}

class TypedParameterVariantGenerator[T <: ProcessStub[_, _]](initWithDefaults: Boolean = false)(implicit classTag: ClassTag[T]) extends ParameterVariantGenerator[T] {
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
	def generate(x: List[List[AnyRef]]): List[List[AnyRef]] =
		x match {
			case Nil => List(Nil)
			case h :: t => for (j <- generate(t); i <- h) yield i :: j
		}
}