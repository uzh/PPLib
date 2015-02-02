package ch.uzh.ifi.pdeboer.pplib.process

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 28/11/14.
 */
@SerialVersionUID(1l) class ProcessParameter[T: ClassTag](keyPostfix: String, val candidateDefinitions: Option[Iterable[T]] = None) extends Serializable {
	def key = keyPostfix

	def clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass

	def t = implicitly[ClassTag[T]]

	def get(implicit processStub: ProcessStub[_, _]) = processStub.getParam(this)

	override def toString: String = key
}