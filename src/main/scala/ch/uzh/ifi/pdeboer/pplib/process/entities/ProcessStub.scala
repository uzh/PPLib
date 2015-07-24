package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._


/**
 * Created by pdeboer on 13/02/15.
 */
object ProcessStub {
	def createFromBlueprint[P <: ProcessStub[_, _]](clazz: Class[P], params: Map[String, Any], factory: ProcessFactory[P]): P = {
		//if Java Annotation factory was used --> convert to default
		val finalFactory = if (factory.isInstanceOf[JavaAnnotationCompatibleDefaultProcessFactoryWrapper])
			new DefaultProcessFactory[P](clazz)
		else factory
		finalFactory.buildProcess(params)
	}

	def createFromBlueprint[P <: ProcessStub[_, _]](clazz: Class[P], params: Map[String, Any]): P = {
		val annotation: PPLibProcess = clazz.getAnnotation(classOf[PPLibProcess])
		val factory = if (annotation != null) annotation.builder().newInstance().asInstanceOf[ProcessFactory[P]]
		else {
			new DefaultProcessFactory[P](clazz)
		}
		createFromBlueprint[P](clazz, params, factory)
	}

	def create[BASE <: ProcessStub[_, _]](params: Map[String, Any])(implicit cls: ClassTag[BASE]): BASE = {
		createFromBlueprint[BASE](cls.runtimeClass.asInstanceOf[Class[BASE]], params)
	}

	def create[BASE <: ProcessStub[_, _]](params: Map[String, Any], factory: ProcessFactory[BASE])(implicit cls: ClassTag[BASE]): BASE = {
		createFromBlueprint[BASE](cls.runtimeClass.asInstanceOf[Class[BASE]], params, factory)
	}
}

/**
 * Created by pdeboer on 09/10/14.
 *
 * Base class for Recombination. Things to keep in mind:
 * <li>Your subclass should have a constructor that accepts an empty Map[String,Any] as parameter for RecombinationParameterGeneration to work</li>
 * <li>If you would like to use automatic initialization, use the @RecombinationProcess annotation and make sure your process works out of the box without any parameters</li>
 */
@SerialVersionUID(1l) abstract class ProcessStub[INPUT, OUTPUT](var params: Map[String, Any])(implicit val inputClass: ClassTag[INPUT], val outputClass: ClassTag[OUTPUT], val inputType: TypeTag[INPUT], val outputType: TypeTag[OUTPUT]) extends LazyLogger with IParametrizable with Serializable {

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

	implicit val processStub = this

	def setParam(key: String, value: Any) {
		params += key -> value
	}

	def getProcessMemoizer(identity: String) = MEMOIZER_NAME.get match {
		case Some(x: String) => Some(new FileProcessMemoizer(x + identity))
		case _ => None
	}

	protected var _results = collection.mutable.HashMap.empty[INPUT, OUTPUT]

	def results: Map[INPUT, OUTPUT] = _results.toMap


	def process(data: INPUT): OUTPUT = {
		ensureExpectedParametersGiven(expectedParametersBeforeRun)

		logger.info(s"running process ${getClass.getSimpleName}")
		val result: OUTPUT = run(data)

		if (getParam(STORE_EXECUTION_RESULTS)) _results += data -> result

		result
	}

	def getCostCeiling(data: INPUT): Int = {
		logger.info(s"No cost ceiling function implemented for ${getClass.getName}, returning zero-cost")
		0
	}

	def ensureExpectedParametersGiven(expected: List[ProcessParameter[_]]): Unit = {
		expected.forall(k => params.get(k.key) match {
			case Some(v) => isParameterTypeCorrect(k.key, v)
			case None => throw new IllegalArgumentException("Parameter not defined: " + k.key + ":" + k.clazz.getCanonicalName)
		})
	}

	protected def run(data: INPUT): OUTPUT

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

	def allParams: List[ProcessParameter[_]] = {
		expectedParametersOnConstruction ::: expectedParametersBeforeRun :::
			optionalParameters ::: defaultParameters
	}

	def allParameterTypesCorrect: Boolean = {
		params.forall {
			case (key, value) => isParameterTypeCorrect(key, value)
		}
	}

	def getParamOption[T](param: ProcessParameter[T], useDefaultValues: Boolean = true): Option[T] = {
		params.get(param.key) match {
			case Some(p) => Some(p.asInstanceOf[T])
			case _ => if (useDefaultValues) param.candidateDefinitions.getOrElse(Nil).headOption
			else None
		}
	}

	def getParam[T](param: ProcessParameter[T], useDefaultValues: Boolean = true): T =
		getParamOption[T](param, useDefaultValues).get

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
		<InputClass>
			{inputClass.runtimeClass.getCanonicalName}
		</InputClass>
		<OutputClass>
			{outputClass.runtimeClass.getCanonicalName}
		</OutputClass>
		<Parameters>
			{allParams.map(p => {
			<Parameter>
				<Name>
					{p.key}
				</Name>
				<Value>
					{getParam(p, useDefaultValues = true)}
				</Value>
				<IsSpecified>
					{params.contains(p.key)}
				</IsSpecified>
			</Parameter>
		})}
		</Parameters>
	</Process>

	ensureExpectedParametersGiven(expectedParametersOnConstruction)
	if (allParams.map(_.key).toSet.size
		!= allParams.map(_.key).size) {
		println("bad")
	}
	assert(allParams.map(_.key).toSet.size
		== allParams.map(_.key).size, "Please assign a unique key to every parameter of this process")

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

	override def toString(): String = s"${
		getClass.getSimpleName
	} ( ${
		params.toString()
	}} )"
}