package ch.uzh.ifi.pdeboer.crowdlang.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
//TODO get rid of ugly _,_,_
class RecombinationVariantGenerator(configs: Map[String, List[RecombinationStub[_, _, _, _]]]) {
	lazy val variants = {
		val listOfTupleLists: List[List[(String, RecombinationStub[_, _, _, _])]] = configs.keys.map(k => configs(k).map(r => (k, r)).toList).toList
		combinationGenerator(listOfTupleLists).map(k => RecombinationVariant(k.toMap))
	}

	/**
	 * taken from http://stackoverflow.com/questions/23425930/generating-all-possible-combinations-from-a-listlistint-in-scala
	 * and adapted
	 * @param x
	 * @return
	 */
	protected def combinationGenerator(x: List[List[(String, RecombinationStub[_, _, _, _])]]):
	List[List[(String, RecombinationStub[_, _, _, _])]] = x match {
		case Nil => List(Nil)
		case h :: t => for (j <- combinationGenerator(t); i <- h) yield i :: j
	}
}
