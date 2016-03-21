package ch.uzh.ifi.pdeboer.pplib.process.entities

import java.lang.reflect.Constructor

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

import scala.collection.parallel.ParSeq
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._


abstract class CreateProcess[INPUT, OUTPUT](params: Map[String, Any])(implicit inputClass: ClassTag[INPUT], outputClass: ClassTag[OUTPUT], inputType1: TypeTag[INPUT], outputType1: TypeTag[OUTPUT]) extends ProcessStub[INPUT, OUTPUT](params) {
	def dataSizeMultiplicator: Int = 1

	override def processInstructionGenerator: Option[InstructionGenerator] = Some(new SimpleInstructionGeneratorCreate())
}

abstract class DecideProcess[INPUT, OUTPUT](params: Map[String, Any])(implicit inputClass: ClassTag[INPUT], outputClass: ClassTag[OUTPUT], inputType1: TypeTag[INPUT], outputType1: TypeTag[OUTPUT]) extends ProcessStub[INPUT, OUTPUT](params) {
	override def processInstructionGenerator: Option[InstructionGenerator] = Some(new SimpleInstructionGeneratorDecide())
}

trait ProcessFactory[BASE <: ProcessStub[_, _]] {
	def buildProcess(params: Map[String, Any] = Map.empty): BASE = typelessBuildProcess(params).asInstanceOf[BASE]

	def typelessBuildProcess(params: Map[String, Any]): ProcessStub[_, _]
}

class GenericsEnabledProcessFactory[BASE <: ProcessStub[_, _]]()(implicit cls: ClassTag[BASE]) extends DefaultProcessFactory[BASE](null) {
	override def clazz = cls.runtimeClass.asInstanceOf[Class[BASE]]
}

class DefaultProcessFactory[BASE <: ProcessStub[_, _]](_clazz: Class[BASE]) extends ProcessFactory[BASE] {
	def clazz: Class[BASE] = _clazz

	override def typelessBuildProcess(params: Map[String, Any]): ProcessStub[_, _] = {
		val targetConstructor: Constructor[_] = clazz.getDeclaredConstructor(classOf[Map[String, Any]])
		targetConstructor.newInstance(params).asInstanceOf[ProcessStub[_, _]]
	}
}

final class JavaAnnotationCompatibleDefaultProcessFactoryWrapper extends ProcessFactory[ProcessStub[_, _]] {
	override def typelessBuildProcess(params: Map[String, Any]): ProcessStub[_, _] = {
		throw new IllegalAccessException("Please call me through ProcessStub.create()")
		???
	}
}

trait HCompPortalAccess extends IParametrizable {
	self: ProcessStub[_, _] =>

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
	import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

	lazy val portal = new CostCountingEnabledHCompPortal(self.getParam(DefaultParameters.PORTAL_PARAMETER))

	def getCrowdWorkers(workerCount: Int): ParSeq[Int] = {
		if (workerCount < 1) List.empty[Int].view.mpar else (1 to workerCount).view.mpar
	}

	override def defaultParameters: List[ProcessParameter[_]] = combineParameterLists(List(PARALLEL_EXECUTION_PARAMETER, PORTAL_PARAMETER), super.defaultParameters)
}

trait InstructionHandler extends IParametrizable {
	self: ProcessStub[_, _] =>

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

	def instructions: QuestionRenderer =
		instructionGenerator.generateQuestion(self.getParam(INSTRUCTIONS))

	def instructionTitle: String = instructionGenerator.generateQuestionTitle(self.getParam(INSTRUCTIONS))

	def instructionGenerator: InstructionGenerator = {
		self.getParam(OVERRIDE_INSTRUCTION_GENERATOR).getOrElse(generatorFromPool.getOrElse(defaultInstructionGenerator))
	}

	private def baseTpe[BASE <: ProcessStub[_, _]](self: BASE)(implicit baseType: TypeTag[BASE], baseClass: ClassTag[BASE]) = baseType

	def generatorFromPool: Option[InstructionGenerator] = {
		val me = runtimeMirror(this.getClass.getClassLoader).classSymbol(getClass).toType
		val foundGenerators = self.getParam(INSTRUCTION_GENERATOR_POOL).find(i => me <:< i._1)
		foundGenerators.map(_._2.head)
	}

	def defaultInstructionGenerator: InstructionGenerator =
		self.processInstructionGenerator.get

	override def defaultParameters: List[ProcessParameter[_]] = {
		combineParameterLists(List(OVERRIDE_INSTRUCTION_GENERATOR, QUESTION_AUX, QUESTION_PRICE), super.defaultParameters)
	}

	override def optionalParameters: List[ProcessParameter[_]] = {
		val superParam: List[ProcessParameter[_]] = try {
			super.optionalParameters
		}
		catch {
			case e: AbstractMethodError => Nil

			/**
			  * AbstractMethodError occurs if class extends existing class with this trait, hence
			  * trait is applied multiple times in a row. This is ok and expected
			  */
		}
		combineParameterLists(List(INSTRUCTIONS), superParam)
	}
}

trait QueryInjection extends IParametrizable {
	self: ProcessStub[_, _] =>

	def createComposite(baseQuery: List[HCompQuery]): CompositeQuery = {
		val main = baseQuery.head
		CompositeQuery(baseQuery ::: INJECT_QUERIES.get.values.toList,
			main.question, main.title)
	}

	def createComposite(baseQuery: HCompQuery): CompositeQuery = createComposite(List(baseQuery))

	private def getQueryAnswersFromComposite(compositeAnswer: CompositeQueryAnswer, needle: Map[String, HCompQuery]): Map[String, HCompAnswer] =
		needle.map(q => q._1 -> compositeAnswer.get[HCompAnswer](q._2))

	def addInjectedAnswersToPatch(patch: Patch, compositeAnswers: List[CompositeQueryAnswer], additionalQueryData: Map[String, HCompQuery] = Map.empty): Unit = {
		val map: List[Map[String, HCompAnswer]] = compositeAnswers.map(c =>
			getQueryAnswersFromComposite(c, INJECT_QUERIES.get) ++
				getQueryAnswersFromComposite(c, additionalQueryData))
		if (map.nonEmpty) {
			patch.auxiliaryInformation += ("answersForInjectedQueries" -> map)
		}
	}

	def addInjectedAnswersToPatch(patch: Patch, compositeAnswer: CompositeQueryAnswer): Unit = {
		addInjectedAnswersToPatch(patch, List(compositeAnswer))
	}

	override def defaultParameters: List[ProcessParameter[_]] = {
		combineParameterLists(List(INJECT_QUERIES), super.defaultParameters)
	}
}
