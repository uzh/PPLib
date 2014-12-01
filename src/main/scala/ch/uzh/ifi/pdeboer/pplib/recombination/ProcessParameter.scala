package ch.uzh.ifi.pdeboer.pplib.recombination

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 28/11/14.
 */
class ProcessParameter[T: ClassTag](keyPostfix: String, val parameterCategory: ProcessParameterCategory = OtherParam(), val candidateDefinitions: Option[Iterable[T]] = None) {
	def key = parameterCategory + "_" + keyPostfix

	def clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass

	def t = implicitly[ClassTag[T]]

	def get(implicit processStub: ProcessStub[_, _]) = processStub.getParam(this)
}

class ProcessParameterCategory(val parameterPrefix: String) {
	ProcessParameterCategory.add(this)

	override def toString = parameterPrefix
}

object ProcessParameterCategory {
	private var paramCategories = Map.empty[String, ProcessParameterCategory]

	def add(cat: ProcessParameterCategory): Unit = {
		paramCategories += cat.parameterPrefix -> cat
	}

	def getAll = paramCategories.toMap
}

case class WorkerCountParam() extends ProcessParameterCategory("workercount")

case class PortalParam() extends ProcessParameterCategory("portal")

case class QuestionParam() extends ProcessParameterCategory("question")

case class WorkflowParam() extends ProcessParameterCategory("process")

case class OtherParam() extends ProcessParameterCategory("other")