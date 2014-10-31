package ch.uzh.ifi.pdeboer.pplib.recombination

import scala.collection.mutable

/**
 * Created by pdeboer on 20/10/14.
 */
object RecombinationDB {
	private var db = new mutable.HashMap[String, RecombinationCategory]()

	def get(category: String) = db(category)

	def put(category: String, stub: RecombinationStub[_, _]): Unit = {
		val cat = db.getOrElse(category, RecombinationCategory(category))
		cat.addStub(stub)
		db += (category -> cat)
	}
}

case class RecombinationCategory(name: String) {
	private var _stubs: mutable.Set[RecombinationStub[_, _]] = mutable.HashSet.empty[RecombinationStub[_, _]]

	def addStub(s: RecombinationStub[_, _]): Unit = {
		_stubs += s
	}

	def stubs = _stubs.toSet
}