package ch.uzh.ifi.pdeboer.crowdlang.recombination

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 09/10/14.
 */
//TODO use param class for map
abstract class RecombinationStub[+INPUT, +OUTPUT](
													 var params: Map[String, AnyRef] = Map.empty[String, AnyRef],
													 val recombinationCategories: List[String] = Nil
													 ) {
	/**
	 * central method of recombination.
	 * @param data
	 * @tparam I
	 * @tparam O
	 * @return
	 */
	def run[I >: INPUT, O >: INPUT](data: I): O

	def isParameterTypeCorrect(key: String, value: AnyRef): Boolean = {
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

	//TODO: should we defer this to an init method that's only executed when running?
	assert(expectedParameters.forall(k => {
		val definedParam: Option[AnyRef] = params.get(k.key)
		val r = definedParam.isDefined && isParameterTypeCorrect(k.key, definedParam.getOrElse(None))
		if (!r)
			throw new IllegalArgumentException("Parameter not defined or type wrong: " + k.key + ":" + k.clazz.getCanonicalName)
		r
	}))
}

class OnlineRecombination[I, O](val identifier: String) extends Iterable[RecombinationStub[I, O]] {
	override def iterator: Iterator[RecombinationStub[I, O]] = RecombinationDB.get(identifier).asInstanceOf[RecombinationCategory[I, O]].stubs.iterator
}

case class RecombinationParameter[T: ClassTag](key: String, candidateDefinitions: Option[Iterable[T]] = None) {
	def clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass
}