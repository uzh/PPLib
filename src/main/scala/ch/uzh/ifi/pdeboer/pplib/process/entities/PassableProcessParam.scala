package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.process.{ProcessFactory, ProcessStub}

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 14/12/14.
 */
class PassableProcessParam[IN: ClassTag, OUT: ClassTag](val clazz: Class[_ <: ProcessStub[IN, OUT]],
														var params: Map[String, Any] = Map.empty,
														val factory: Option[ProcessFactory] = None) {
	protected var _createdProcesses = List.empty[ProcessStub[IN, OUT]]

	def create(lowerPrioParams: Map[String, Any] = Map.empty, higherPrioParams: Map[String, Any] = Map.empty): ProcessStub[IN, OUT] = {
		val res = ProcessStub.create[IN, OUT](clazz, (lowerPrioParams ++ params) ++ higherPrioParams, factory)
		_createdProcesses = res :: _createdProcesses
		res
	}

	def createdProcesses = _createdProcesses

	def getParam[T](key: String) = params.get(key).asInstanceOf[Option[T]]
}
