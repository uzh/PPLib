package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub

/**
 * Created by pdeboer on 17/02/15.
 */
class RecombinationDB {
	private var classes = collection.mutable.HashSet.empty[Class[_ >: ProcessStub[_, _]]]
	/*
		def addClass(cls:Class[_ >: ProcessStub[_,_]): Unit = {
			classes += cls
		}
		*/
}

object RecombinationDB {
	val DEFAULT = new RecombinationDB

	//load all annotated classes and add them here
}