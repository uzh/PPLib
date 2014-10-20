package ch.uzh.ifi.pdeboer.crowdlang.recombination

import scala.collection.mutable

/**
 * Created by pdeboer on 20/10/14.
 */
object RecombinationDB {
	private var db = new mutable.HashMap[String, RecombinationCategory[Any, Any]]()

	def get(category: String) = db(category)

	def put(category: String, stub: RecombinationStub[_, _]): Unit = {
		val cat = db.getOrElse(category, RecombinationCategory[Any, Any](category))
		cat.addStub(stub)
		db += (category -> cat)
	}
}

case class RecombinationCategory[I, O](name: String) {
	private var _stubs: mutable.Set[RecombinationStub[I, O]] = mutable.HashSet.empty[RecombinationStub[I, O]]

	def addStub(s: RecombinationStub[I, O]): Unit = {
		_stubs += s
	}

	def stubs = _stubs.toSet
}