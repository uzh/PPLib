package ch.uzh.ifi.pdeboer.crowdlang.recombination

import scala.collection.mutable

/**
 * Created by pdeboer on 09/10/14.
 */
//TODO get rid of ugly _,_,_
class RecombinationVariantGenerator[I, O](configs: Map[String, List[RecombinationStub[I, _, _, O]]]) {
	lazy val variants = {
		val listOfTupleLists: List[List[(String, RecombinationStub[I, _, _, O])]] = configs.map(k => k._2.map(r => (k._1, r)).toList).toList
		CombinationGenerator.generate(listOfTupleLists).map(k => {
			RecombinationVariant(k.asInstanceOf[List[(String, RecombinationStub[_, _, _, _])]].toMap)
		})
	}
}

class RecombinationStubParameterVariantGenerator[I, O](val base: RecombinationStub[I, _, _, O]) {
	private var parameterValues = new mutable.HashMap[String, mutable.Set[Any]]()

	def initAllParamsWithCandidates(): Unit = {
		val expected = base.expectedParameters
		expected.foreach(k => addParameterVariations(k.key, k.candidateDefinitions.getOrElse(Nil).toList))
	}

	def addParameterVariations(paramKey: String, values: List[Any]): Unit = {
		var cur = parameterValues.getOrElse(paramKey, mutable.HashSet.empty[Any])
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