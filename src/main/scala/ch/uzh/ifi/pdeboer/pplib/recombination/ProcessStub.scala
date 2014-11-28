package ch.uzh.ifi.pdeboer.pplib.recombination

import ch.uzh.ifi.pdeboer.pplib.hcomp.{CostCountingEnabledHCompPortal, CrowdWorker, HComp, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.util.U
import com.typesafe.scalalogging.LazyLogging

import scala.collection.parallel.{ForkJoinTaskSupport, ParSeq}
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

	import ch.uzh.ifi.pdeboer.pplib.recombination.ProcessStub._

	protected var _results = collection.mutable.HashMap.empty[INPUT, OUTPUT]

	def results: Map[INPUT, OUTPUT] = _results.toMap

	lazy val inputType = implicitly[ClassTag[INPUT]]
	lazy val outputType = implicitly[ClassTag[OUTPUT]]

	def process(data: INPUT): OUTPUT = {
		ensureExpectedParametersGiven(expectedParametersBeforeRun)

		logger.info(s"running process ${getClass.getSimpleName}")
		val result: OUTPUT = run(data)

		if (getParamUnsafe(STORE_EXECUTION_RESULTS)) _results += data -> result

		result
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

	protected def processCategoryNames: List[String] = Nil

	final def processCategories = {
		val names: List[String] =
			if (processCategoryNames == null)
				Nil
			else processCategoryNames
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

	def xml = <Process>
		<Class>
			{getClass.getName}
		</Class>
		<Input>
			{inputType.runtimeClass.getCanonicalName}
		</Input>
		<Output>
			{outputType.runtimeClass.getCanonicalName}
		</Output>
		<Categories>
			{processCategories.map(c => {
			<Category>
				{c.path}
			</Category>
		})}
		</Categories>
		<Parameters>
			{allParams.map(p => {
			<Parameter>
				<Name>
					{p.key}
				</Name>
				<Category>
					{p.parameterCategory}
				</Category>
				<Value>
					{getParamUnsafe(p, useDefaultValues = true)}
				</Value>
				<IsSpecified>
					{params.contains(p.key)}
				</IsSpecified>
			</Parameter>
		})}
		</Parameters>
	</Process>

	ensureExpectedParametersGiven(expectedParametersOnConstruction)
	processCategories.foreach(c => ProcessDB.put(c, this))

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

object ProcessStub {
	val STORE_EXECUTION_RESULTS = new ProcessParameter[Boolean]("storeExecutionResults", OtherParam(), Some(List(true)))
}

abstract class ProcessStubWithHCompPortalAccess[INPUT: ClassTag, OUTPUT: ClassTag](params: Map[String, Any] = Map.empty[String, AnyRef]) extends ProcessStub[INPUT, OUTPUT](params) {

	import ch.uzh.ifi.pdeboer.pplib.recombination.ProcessStubWithHCompPortalAccess._

	lazy val portal = new CostCountingEnabledHCompPortal(getParamUnsafe(PORTAL_PARAMETER))

	//TODO test if this actually works and doesnt destroy parallelism
	def getCrowdWorkers(workerCount: Int) = (if (getParamUnsafe(PARALLEL_EXECUTION_PARAMETER)) {
		val par: ParSeq[Int] = (1 to workerCount).view.par
		par.tasksupport = new ForkJoinTaskSupport(U.hugeForkJoinPool)
		par
	} else (1 to workerCount).view).map(i => new CrowdWorker(i + ""))

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = PORTAL_PARAMETER :: super.expectedParametersBeforeRun

	override def optionalParameters: List[ProcessParameter[_]] = PARALLEL_EXECUTION_PARAMETER :: super.optionalParameters
}

object ProcessStubWithHCompPortalAccess {
	val PORTAL_PARAMETER = new ProcessParameter[HCompPortalAdapter]("portal", PortalParam(), Some(HComp.allDefinedPortals))
	val PARALLEL_EXECUTION_PARAMETER = new ProcessParameter[Boolean]("parallel", OtherParam(), Some(List(true)))
}

