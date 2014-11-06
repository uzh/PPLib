package ch.uzh.ifi.pdeboer.pplib.recombination

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Created by pdeboer on 20/10/14.
 */
object RecombinationDB {
	private var db = new mutable.HashMap[String, RecombinationCategory]()

	def getByKey(category: String): RecombinationCategory = db(category)

	def get[IN, OUT](simpleName: String) =
		db(RecombinationCategory.generateKey(simpleName)).stubs.toList

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

object RecombinationCategory {
	def generateKey[INPUT: ClassTag, OUTPUT: ClassTag](name: String): String =
		s"in:${implicitly[ClassTag[INPUT]].runtimeClass.getSimpleName},out:${implicitly[ClassTag[OUTPUT]].runtimeClass.getSimpleName},name:$name"
}