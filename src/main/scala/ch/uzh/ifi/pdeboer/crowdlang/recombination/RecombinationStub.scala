package ch.uzh.ifi.pdeboer.crowdlang.recombination

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 09/10/14.
 */
class RecombinationStub[INPUT, PROCESS_INPUT, PROCESS_OUTPUT, OUTPUT](
																		 val inputPreprocessor: INPUT => PROCESS_INPUT, processor: PROCESS_INPUT => PROCESS_OUTPUT,
																		 val outputPostprocessor: PROCESS_OUTPUT => OUTPUT,
																		 val recombinationCategories: List[String] = Nil,
																		 var params: Map[String, Any] = Map.empty[String, Any]
																		 ) {

	def run(data: INPUT): OUTPUT =
		outputPostprocessor(processor(inputPreprocessor(data)))

	def isParameterTypeCorrect(key: String, value: Any): Boolean = {
		val allParams = expectedParameters ::: optionalParameters
		val e = allParams.find(_.key.equals(key))
		if (e.isDefined) {
			value.getClass.isAssignableFrom(e.get.clazz)
		} else false
	}

	/**
	 * override this method to ensure the definition of some parameters.
	 * Expected parameters of type "Option" will default to NONE if checked against.
	 * @return
	 */
	def expectedParameters = List.empty[RecombinationParameter[_]]

	def optionalParameters = List.empty[RecombinationParameter[_]]

	assert(expectedParameters.forall(k => {
		val definedParam: Option[Any] = params.get(k.key)
		val r = definedParam.isDefined && isParameterTypeCorrect(k.key, definedParam.getOrElse(None))
		if (!r)
			throw new IllegalArgumentException("Parameter not defined or type wrong: " + k.key + ":" + k.clazz.getCanonicalName)
		r
	}))
}

class OnlineRecombination(val identifier: String) extends Iterable[RecombinationStub[_, _, _, _]] {
	override def iterator: Iterator[RecombinationStub[_, _, _, _]] = RecombinationDB.get(identifier).stubs.iterator
}

case class RecombinationParameter[T: ClassTag](key: String, candidateDefinitions: Option[Iterable[T]] = None) {
	def clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass
}

class SimpleRecombinationStub[PROCESS_INPUT, PROCESS_OUTPUT](
																val processor: PROCESS_INPUT => PROCESS_OUTPUT,
																recombinationCategories: List[String] = Nil,
																params: Map[String, AnyRef] = Map.empty[String, AnyRef]
																) extends RecombinationStub[PROCESS_INPUT, PROCESS_INPUT,
	PROCESS_OUTPUT, PROCESS_OUTPUT](i => i, processor, o => o, recombinationCategories, params)
