package ch.uzh.ifi.pdeboer.pplib.recombination

import ch.uzh.ifi.pdeboer.pplib.hcomp.{CostCountingEnabledHCompPortal, HComp, HCompPortalAdapter}

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

/**
 * Created by pdeboer on 09/10/14.
 */
abstract class RecombinationStub[INPUT: ClassTag, OUTPUT: ClassTag](var params: Map[String, Any] = Map.empty[String, AnyRef]) {
	final def types: (ClassTag[INPUT], ClassTag[OUTPUT]) = (implicitly[ClassTag[INPUT]], implicitly[ClassTag[OUTPUT]])

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
	def expectedParametersOnConstruction: List[RecombinationParameter[_]] = List.empty[RecombinationParameter[_]]

	def expectedParametersBeforeRun: List[RecombinationParameter[_]] = List.empty[RecombinationParameter[_]]

	def optionalParameters: List[RecombinationParameter[_]] = List.empty[RecombinationParameter[_]]

	protected def recombinationCategoryNames: List[String] = Nil

	final def recombinationCategories = {
		val names: List[String] =
			if (recombinationCategoryNames == null)
				Nil
			else recombinationCategoryNames
		val annotation =
			if (this.getClass.isAnnotationPresent(classOf[RecombinationProcess]))
				this.getClass.getAnnotation(classOf[RecombinationProcess]).value()
			else ""
		(annotation :: names).map(n => RecombinationCategory.get[INPUT, OUTPUT](n))
	}


	final def isParameterTypeCorrect(key: String, value: Any): Boolean = {
		val e = allParams.find(_.key.equals(key))
		val ret = if (e.isDefined) {
			val left = value.getClass
			val right = e.get.clazz
			left.isAssignableFrom(right) // TODO doesnt work
			true
		} else false
		ret
	}

	final def isApproxSubType[T: Manifest, U: Manifest] = manifest[T] <:< manifest[U]

	def allParams: List[RecombinationParameter[_]] = {
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

	/*
	assert(allParameterTypesCorrect,
		s"some parameter types were not correct: ${
			allParams
				.filter(p => params.contains(p.key))
				.filterNot(p => isParameterTypeCorrect(p.key, params(p.key)))
				.map(p => s"${p.key} is ${params(p.key).getClass.getCanonicalName}, should be ${p.clazz.getCanonicalName}").mkString(", ")
		}")
	*/
	ensureExpectedParametersGiven(expectedParametersOnConstruction)
	recombinationCategories.foreach(c => RecombinationDB.put(c, this))

	def canEqual(other: Any): Boolean = other.isInstanceOf[RecombinationStub[_, _]]

	override def equals(other: Any): Boolean = other match {
		case that: RecombinationStub[_, _] =>
			(that canEqual this) &&
				params == that.params &&
				this.getClass == other.getClass
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(params)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}

abstract class RecombinationStubWithHCompPortalAccess[INPUT: ClassTag, OUTPUT: ClassTag](params: Map[String, Any] = Map.empty[String, AnyRef]) extends RecombinationStub[INPUT, OUTPUT](params) {
	lazy val portal = new CostCountingEnabledHCompPortal(getParamUnsafe(PORTAL))

	override def expectedParametersBeforeRun: List[RecombinationParameter[_]] = PORTAL :: super.expectedParametersBeforeRun

	val PORTAL = new RecombinationParameter[HCompPortalAdapter]("portal", Some(HComp.allDefinedPortals))
}

object RecombinationStubWithHCompPortalAccess {
	val PORTAL_PARAMETER = new RecombinationParameter[HCompPortalAdapter]("portal", Some(HComp.allDefinedPortals))
}

class OnlineRecombination[I: ClassTag, O: ClassTag](val path: String, includeChildren: Boolean = false) extends Iterable[RecombinationStub[I, O]] {
	override def iterator: Iterator[RecombinationStub[I, O]] = RecombinationDB.get[I, O](path, includeChildren).iterator.asInstanceOf[Iterator[RecombinationStub[I, O]]]
}

class RecombinationParameter[T: ClassTag](val key: String, val candidateDefinitions: Option[Iterable[T]] = None) {
	def clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass

	def t = implicitly[ClassTag[T]]
}