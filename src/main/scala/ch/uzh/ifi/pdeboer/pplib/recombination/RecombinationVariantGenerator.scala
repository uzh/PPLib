package ch.uzh.ifi.pdeboer.pplib.recombination

import scala.collection.mutable

/**
 * Created by pdeboer on 09/10/14.
 */
class RecombinationVariantGenerator(configs: Map[String, List[RecombinationStub[_, _]]]) {
	lazy val variants = {
		val listOfTupleLists: List[List[(String, RecombinationStub[_, _])]] = configs.map(k => k._2.map(r => (k._1, r)).toList).toList
		CombinationGenerator.generate(listOfTupleLists).map(k => {
			RecombinationVariant(k.asInstanceOf[List[(String, RecombinationStub[_, _])]].toMap)
		})
	}
}

class RecombinationStubParameterVariantGenerator[I >: AnyRef, O >: AnyRef](val base: RecombinationStub[I, O]) {
	protected var parameterValues = new mutable.HashMap[String, mutable.Set[AnyRef]]()

	def initAllParamsWithCandidates(): Unit = {
		val expected = base.expectedParameters ::: base.optionalParameters
		expected.foreach(k => addParameterVariations(k.key, k.candidateDefinitions.getOrElse(Nil).toList.asInstanceOf[List[AnyRef]]))
	}

	def addParameterVariations(paramKey: String, values: List[AnyRef]): Unit = {
		var cur = parameterValues.getOrElse(paramKey, mutable.HashSet.empty[AnyRef])
		values.foreach(k => {
			if (base.isParameterTypeCorrect(paramKey, k))
				cur += k
			else throw new IllegalArgumentException("Parameter type incorrect for " + paramKey)
		})
		parameterValues += (paramKey -> cur)
	}

	def generateParameterVariations() = {
		val listOfTupleLists = parameterValues.map(k => k._2.map(r => (k._1, r)).toList).toList
		CombinationGenerator.generate(listOfTupleLists).map(k => {
			k.asInstanceOf[List[(String, Any)]].toMap
		})
	}
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