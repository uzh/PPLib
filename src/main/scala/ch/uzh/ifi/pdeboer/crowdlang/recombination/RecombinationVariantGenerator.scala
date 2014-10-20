package ch.uzh.ifi.pdeboer.crowdlang.recombination

import scala.collection.mutable

/**
 * Created by pdeboer on 09/10/14.
 */
//TODO get rid of ugly _,_,_
class RecombinationVariantGenerator[I, O](configs: Map[String, List[RecombinationStub[I, _, _, O]]]) {
	lazy val variants = {
		val listOfTupleLists: List[List[(String, RecombinationStub[I, _, _, O])]] = configs.keys.map(k => configs(k).map(r => (k, r)).toList).toList
		combinationGenerator(listOfTupleLists).map(k => RecombinationVariant(k.toMap))
	}

	/**
	 * taken from http://stackoverflow.com/questions/23425930/generating-all-possible-combinations-from-a-listlistint-in-scala
	 * and adapted
	 * @param x
	 * @return
	 */
	protected def combinationGenerator(x: List[List[(String, RecombinationStub[I, _, _, O])]]):
	List[List[(String, RecombinationStub[I, _, _, O])]] = x match {
		case Nil => List(Nil)
		case h :: t => for (j <- combinationGenerator(t); i <- h) yield i :: j
	}
}

class RecombinationStubParameterVariantGenerator[I, O](val base: RecombinationStub[I, _, _, O]) {
	private var parameterValues = new mutable.HashMap[String, List[Any]]()

	def initAllParamsWithCandidates: Unit = {
		val expected = base.expectedParameters
		expected.foreach(k => addParameterVariations(k.key, k.candidateDefinitions.getOrElse(Nil).toList))
	}

	def addParameterVariations(paramKey: String, values: List[Any]): Unit = {
		val cur = values ::: parameterValues.getOrElse(paramKey, List.empty[Any])
		parameterValues += (paramKey -> cur)
	}
}