package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessFactory

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 14/12/14.
 */
class GenericPassableProcessParam[IN: ClassTag, OUT: ClassTag, Base <: ProcessStub[IN, OUT]](val clazz: Class[_ <: Base],
														var params: Map[String, Any] = Map.empty,
														val factory: Option[ProcessFactory] = None) {
	protected var _createdProcesses = List.empty[ProcessStub[IN, OUT]]

	def create(lowerPrioParams: Map[String, Any] = Map.empty,
			   higherPrioParams: Map[String, Any] = Map.empty): ProcessStub[IN, OUT] = {
		val res = ProcessStub.create[IN, OUT](clazz, (lowerPrioParams ++ params) ++ higherPrioParams, factory)
		_createdProcesses = res :: _createdProcesses
		res
	}

	def setParams(p: Map[String, Any], replace: Boolean = false): Unit = {
		if (replace)
			params = params ++ p
		else
			params = p ++ params
	}

	def createdProcesses = _createdProcesses

	def getParam[T](key: String) = params.get(key).asInstanceOf[Option[T]]
}

class PassableProcessParam[IN: ClassTag, OUT: ClassTag](clazz: Class[_ <: ProcessStub[IN, OUT]],
														params: Map[String, Any] = Map.empty,
														factory: Option[ProcessFactory] = None)
	extends GenericPassableProcessParam[IN, OUT, ProcessStub[IN, OUT]](clazz, params, factory)