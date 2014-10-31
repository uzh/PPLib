package ch.uzh.ifi.pdeboer.pplib.recombination

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 09/10/14.
 */
abstract class RecombinationStub[INPUT, OUTPUT](var params: Map[String, AnyRef] = Map.empty[String, AnyRef]) {
	def process(data: INPUT): OUTPUT = {
		ensureExpectedParametersGiven(expectedParametersBeforeRun)

		run(data)
	}

	def ensureExpectedParametersGiven(expected: List[RecombinationParameter[_]]): Unit = {
		expected.forall(k => params.get(k.key) match {
			case Some(v) => isParameterTypeCorrect(k.key, v)
			case None => throw new IllegalArgumentException("Parameter not defined or type wrong: " + k.key + ":" + k.clazz.getCanonicalName)
		})
	}

	protected def run(data: INPUT): OUTPUT

	/**
	 * override this method to ensure the definition of some parameters.
	 * Expected parameters of type "Option" will default to NONE if checked against.
	 * @return
	 */
	def expectedParametersOnConstruction = List.empty[RecombinationParameter[_]]

	def expectedParametersBeforeRun = List.empty[RecombinationParameter[_]]

	def optionalParameters = List.empty[RecombinationParameter[_]]

	def recombinationCategories: List[String] = Nil

	assert(allParameterTypesCorrect)

	ensureExpectedParametersGiven(expectedParametersOnConstruction)

	def isParameterTypeCorrect(key: String, value: AnyRef): Boolean = {
		val e = allParams.find(_.key.equals(key))
		val ret = if (e.isDefined) {
			value.getClass.isAssignableFrom(e.get.clazz)
		} else false
		ret
	}

	final def allParams: List[RecombinationParameter[_]] = {
		expectedParametersOnConstruction ::: expectedParametersBeforeRun ::: optionalParameters
	}

	def allParameterTypesCorrect: Boolean = {
		params.forall {
			case (key, value) => isParameterTypeCorrect(key, value)
		}
	}

	def getParam[T](param: RecombinationParameter[T], useDefaultValues: Boolean = true): Option[T] = {
		params.get(param.key) match {
			case Some(p) => Some(p.asInstanceOf[T])
			case _ => if (useDefaultValues) param.candidateDefinitions.getOrElse(Nil).headOption
			else None
		}
	}

	def getParamByKey[T](param: String, useDefaultValues: Boolean = true): Option[T] = {
		params.get(param) match {
			case Some(p) => Some(p.asInstanceOf[T])
			case _ => if (useDefaultValues) allParams.find(_.key.equals(param)) match {
				case Some(param) => param.candidateDefinitions.getOrElse(Nil).headOption.asInstanceOf[Option[T]]
				case None => None
			}
			else None
		}
	}

	(if (recombinationCategories == null) Nil else recombinationCategories).foreach(c =>
		RecombinationDB.put(c, this))
}

class OnlineRecombination[I, O](val identifier: String) extends Iterable[RecombinationStub[I, O]] {
	override def iterator: Iterator[RecombinationStub[I, O]] =
		RecombinationDB.get(identifier).stubs.iterator.asInstanceOf[Iterator[RecombinationStub[I, O]]]
}

class RecombinationParameter[T: ClassTag](val key: String, val candidateDefinitions: Option[Iterable[T]] = None) {
	def clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass
}