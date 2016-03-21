package ch.uzh.ifi.pdeboer.pplib.process.entities

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
/**
 * Created by pdeboer on 14/12/14.
 */
class PassableProcessParam[Base <: ProcessStub[_, _]](var params: Map[String, Any] = Map.empty,
													  val factory: Option[ProcessFactory[Base]] = None)
													 (implicit baseCls: ClassTag[Base], val baseType: TypeTag[Base]) {
	val clazz = baseCls.runtimeClass
	protected var _createdProcesses = List.empty[Base]

	def create(lowerPrioParams: Map[String, Any] = Map.empty,
			   higherPrioParams: Map[String, Any] = Map.empty): Base = {
		val paramsToUse: Map[String, Any] = (lowerPrioParams ++ params) ++ higherPrioParams
		val res = if (factory.isDefined) {
			ProcessStub.create(paramsToUse, factory.get)
		} else ProcessStub.create(paramsToUse)
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

	def getParam[T](key: String): Option[T] = params.get(key).asInstanceOf[Option[T]]

	def getParam[T](param: ProcessParameter[T]): Option[T] = params.get(param.key).asInstanceOf[Option[T]]

	def clear(): Unit = {
		_createdProcesses = List.empty[Base]
	}

	override def toString = s"PassableProcessParam($baseType, $params)"
}