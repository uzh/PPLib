package ch.uzh.ifi.pdeboer.crowdlang.recombination

import scala.collection.mutable

/**
 * Created by pdeboer on 20/10/14.
 */
object RecombinationDB {
	private var db = new mutable.HashMap[String, RecombinationCategory]()

	def get(category: String) = db(category)

	def put(category: String, stub: RecombinationStub[_, _, _, _]): Unit = {
		val cat = db.getOrElse(category, RecombinationCategory(category))
		cat.addStub(stub)
		db += (category -> cat)
	}
}

case class RecombinationCategory(name: String, private var stubs: mutable.Set[RecombinationStub[_, _, _, _]] = mutable.HashSet.empty[RecombinationStub[_, _, _, _]]) {
	def addStub(s: RecombinationStub[_, _, _, _]): Unit = {
		stubs += s
	}
}