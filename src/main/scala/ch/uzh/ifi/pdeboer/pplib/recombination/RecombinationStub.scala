package ch.uzh.ifi.pdeboer.pplib.recombination

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

/**
 * Created by pdeboer on 09/10/14.
 */
abstract class RecombinationStub[INPUT: ClassTag, OUTPUT: ClassTag](var params: Map[String, Any] = Map.empty[String, AnyRef]) {

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

	protected def recombinationCategoryNames: List[String] = Nil

	def recombinationCategories =
		(if (recombinationCategoryNames == null) Nil else recombinationCategoryNames).
			map(n => s"in:${implicitly[ClassTag[INPUT]].runtimeClass.getSimpleName},out:${implicitly[ClassTag[OUTPUT]].runtimeClass.getSimpleName},name:" + n)

	assert(allParameterTypesCorrect,
		s"some parameter types were not correct: ${
			allParams
				.filter(p => params.contains(p.key))
				.filterNot(p => isParameterTypeCorrect(p.key, params(p.key)))
				.map(p => s"${p.key} is ${params(p.key).getClass.getCanonicalName}, should be ${p.clazz.getCanonicalName}").mkString(", ")
		}")

	ensureExpectedParametersGiven(expectedParametersOnConstruction)

	def isParameterTypeCorrect(key: String, value: Any): Boolean = {
		val e = allParams.find(_.key.equals(key))
		val ret = if (e.isDefined) {
			val left = value.getClass
			val right = e.get.clazz
			left.isAssignableFrom(right) // TODO doesnt work
			true
		} else false
		ret
	}

	def isApproxSubType[T: Manifest, U: Manifest] = manifest[T] <:< manifest[U]

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

	def getParamUnsafe[T](param: RecombinationParameter[T], useDefaultValues: Boolean = true): T =
		getParam[T](param, useDefaultValues).get

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

	def to[IN, OUT] = this.asInstanceOf[RecombinationStub[IN, OUT]]

	recombinationCategories.foreach(c => RecombinationDB.put(c, this))
}

class OnlineRecombination[I, O](val identifier: String) extends Iterable[RecombinationStub[I, O]] {
	override def iterator: Iterator[RecombinationStub[I, O]] =
		RecombinationDB.get(identifier).stubs.iterator.asInstanceOf[Iterator[RecombinationStub[I, O]]]
}

class RecombinationParameter[T: ClassTag](val key: String, val candidateDefinitions: Option[Iterable[T]] = None) {
	def clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass

	def t = implicitly[ClassTag[T]]
}