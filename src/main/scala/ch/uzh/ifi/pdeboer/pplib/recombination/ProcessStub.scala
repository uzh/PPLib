package ch.uzh.ifi.pdeboer.pplib.recombination

import ch.uzh.ifi.pdeboer.pplib.hcomp.{CostCountingEnabledHCompPortal, HComp, HCompPortalAdapter}
import com.typesafe.scalalogging.{LazyLogging, Logger}

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

/**
 * Created by pdeboer on 09/10/14.
 *
 * Base class for Recombination. Things to keep in mind:
 * <li>Your subclass should have a constructor that accepts an empty Map[String,Any] as parameter for RecombinationParameterGeneration to work</li>
 * <li>If you would like to use automatic initialization, use the @RecombinationProcess annotation and make sure your process works out of the box without any parameters</li>
 */
abstract class ProcessStub[INPUT: ClassTag, OUTPUT: ClassTag](var params: Map[String, Any] = Map.empty[String, AnyRef]) extends LazyLogging {
	final def types: (ClassTag[INPUT], ClassTag[OUTPUT]) = (implicitly[ClassTag[INPUT]], implicitly[ClassTag[OUTPUT]])

	def process(data: INPUT): OUTPUT = {
		ensureExpectedParametersGiven(expectedParametersBeforeRun)

		logger.info(s"running process ${getClass.getSimpleName}")
		run(data)
	}

	def ensureExpectedParametersGiven(expected: List[ProcessParameter[_]]): Unit = {
		expected.forall(k => params.get(k.key) match {
			case Some(v) => isParameterTypeCorrect(k.key, v)
			case None => throw new IllegalArgumentException("Parameter not defined: " + k.key + ":" + k.clazz.getCanonicalName)
		})
	}

	protected def run(data: INPUT): OUTPUT

	/**
	 * override this method to ensure the definition of some parameters.
	 * Expected parameters of type "Option" will default to NONE if checked against.
	 * @return
	 */
	def expectedParametersOnConstruction: List[ProcessParameter[_]] = List.empty[ProcessParameter[_]]

	def expectedParametersBeforeRun: List[ProcessParameter[_]] = List.empty[ProcessParameter[_]]

	def optionalParameters: List[ProcessParameter[_]] = List.empty[ProcessParameter[_]]

	protected def recombinationCategoryNames: List[String] = Nil

	final def recombinationCategories = {
		val names: List[String] =
			if (recombinationCategoryNames == null)
				Nil
			else recombinationCategoryNames
		val annotation =
			if (this.getClass.isAnnotationPresent(classOf[PPLibProcess]))
				this.getClass.getAnnotation(classOf[PPLibProcess]).value()
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

	def allParams: List[ProcessParameter[_]] = {
		expectedParametersOnConstruction ::: expectedParametersBeforeRun ::: optionalParameters
	}

	def allParameterTypesCorrect: Boolean = {
		params.forall {
			case (key, value) => isParameterTypeCorrect(key, value)
		}
	}

	def getParam[T](param: ProcessParameter[T], useDefaultValues: Boolean = true): Option[T] = {
		params.get(param.key) match {
			case Some(p) => Some(p.asInstanceOf[T])
			case _ => if (useDefaultValues) param.candidateDefinitions.getOrElse(Nil).headOption
			else None
		}
	}

	def getParamUnsafe[T](param: ProcessParameter[T], useDefaultValues: Boolean = true): T =
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

	def to[IN, OUT] = this.asInstanceOf[ProcessStub[IN, OUT]]

	ensureExpectedParametersGiven(expectedParametersOnConstruction)
	recombinationCategories.foreach(c => ProcessDB.put(c, this))

	def canEqual(other: Any): Boolean = other.isInstanceOf[ProcessStub[_, _]]

	override def equals(other: Any): Boolean = other match {
		case that: ProcessStub[_, _] =>
			(that canEqual this) &&
				params == that.params &&
				this.getClass == other.getClass
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(params)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}

	override def toString(): String = s"${getClass.getSimpleName} ( ${params.toString()}} )"
}

abstract class ProcessStubWithHCompPortalAccess[INPUT: ClassTag, OUTPUT: ClassTag](params: Map[String, Any] = Map.empty[String, AnyRef]) extends ProcessStub[INPUT, OUTPUT](params) {
	lazy val portal = new CostCountingEnabledHCompPortal(getParamUnsafe(PORTAL))

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = PORTAL :: super.expectedParametersBeforeRun

	val PORTAL = new ProcessParameter[HCompPortalAdapter]("portal", Some(HComp.allDefinedPortals))
}

object ProcessStubWithHCompPortalAccess {
	val PORTAL_PARAMETER = new ProcessParameter[HCompPortalAdapter]("portal", Some(HComp.allDefinedPortals))
}

class OnlineRecombination[I: ClassTag, O: ClassTag](val path: String, includeChildren: Boolean = false) extends Iterable[ProcessStub[I, O]] {
	override def iterator: Iterator[ProcessStub[I, O]] = ProcessDB.get[I, O](path, includeChildren).iterator.asInstanceOf[Iterator[ProcessStub[I, O]]]
}

class ProcessParameter[T: ClassTag](val key: String, val candidateDefinitions: Option[Iterable[T]] = None) {
	def clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass

	def t = implicitly[ClassTag[T]]
}

// TODO doesnt work for some reason
object RecombinationParameterConversion {
	implicit def parameterToKey(params: Map[ProcessParameter[_], Any]): Map[String, Any] = params.map {
		case (param, value) => param.key -> value
	}.toMap
}