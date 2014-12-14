package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.process.ProcessStub

/**
 * Created by pdeboer on 14/12/14.
 */
class PassableProcessParam[IN, OUT](val clazz: Class[_ <: ProcessStub[IN, OUT]],
									var params: Map[String, Any] = Map.empty) {
	def create(lowerPrioParams: Map[String, Any] = Map.empty, higherPrioParams: Map[String, Any] = Map.empty): ProcessStub[IN, OUT] = {
		ProcessStub.create[IN, OUT](clazz, (lowerPrioParams ++ params) ++ higherPrioParams)
	}

	def getParam[T](key: String) = params.get(key).asInstanceOf[Option[T]]
}
