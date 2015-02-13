package ch.uzh.ifi.pdeboer.pplib.process.entities

import java.lang.reflect.Constructor

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

import scala.collection.parallel.ParSeq
import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

abstract class CreateProcess[INPUT: ClassTag, OUTPUT: ClassTag](params: Map[String, Any]) extends ProcessStub[INPUT, OUTPUT](params)

abstract class DecideProcess[INPUT: ClassTag, OUTPUT: ClassTag](params: Map[String, Any]) extends ProcessStub[INPUT, OUTPUT](params)

trait ProcessFactory {
	def buildProcess[IN: ClassTag, OUT: ClassTag](params: Map[String, Any] = Map.empty): ProcessStub[IN, OUT] = typelessBuildProcess(params).asInstanceOf[ProcessStub[IN, OUT]]

	def typelessBuildProcess(params: Map[String, Any]): ProcessStub[_, _]
}

class DefaultProcessFactory(baseClass: Class[_ <: ProcessStub[_, _]]) extends ProcessFactory {
	override def buildProcess[IN: ClassTag, OUT: ClassTag](params: Map[String, Any]): ProcessStub[IN, OUT] = {
		//println(baseClass.getDeclaredConstructors.mkString(","))
		typelessBuildProcess(params).asInstanceOf[ProcessStub[IN, OUT]]
	}

	override def typelessBuildProcess(params: Map[String, Any]): ProcessStub[_, _] = {
		val targetConstructor: Constructor[_] = baseClass.getDeclaredConstructor(classOf[Map[String, Any]])
		targetConstructor.newInstance(params).asInstanceOf[ProcessStub[_, _]]
	}
}

trait HCompPortalAccess extends IParametrizable {
	self: ProcessStub[_, _] =>

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
	import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

	lazy val portal = new CostCountingEnabledHCompPortal(self.getParam(DefaultParameters.PORTAL_PARAMETER))

	def getCrowdWorkers(workerCount: Int): ParSeq[Int] = {
		(1 to workerCount).view.mpar
	}

	override def defaultParameters: List[ProcessParameter[_]] = combineParameterLists(List(PARALLEL_EXECUTION_PARAMETER, PORTAL_PARAMETER), super.defaultParameters)
}

trait InstructionHandler extends IParametrizable {
	self: ProcessStub[_, _] =>

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

	def instructions: HCompInstructionsWithTuple =
		instructionGenerator.generateQuestion(self.getParam(INSTRUCTIONS))

	def instructionTitle: String = instructionGenerator.generateQuestionTitle(self.getParam(INSTRUCTIONS))

	def instructionGenerator: InstructionGenerator = {
		val generator = self.getParamOption(OVERRIDE_INSTRUCTION_GENERATOR) match {
			case None => defaultInstructionGenerator.get //TODO yep, this is horrible
			case Some(x: InstructionGenerator) => x
		}
		generator
	}

	def defaultInstructionGenerator: Option[InstructionGenerator] = self match {
		case x: CreateProcess[_, _] => Some(new SimpleInstructionGeneratorCreate())
		case x: DecideProcess[_, _] => Some(new SimpleInstructionGeneratorDecide())
		case _ => None
	}

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
			 * trait is applied multiple times in a row
			 */
		}
		combineParameterLists(List(INSTRUCTIONS), superParam)
	}
}

trait QueryInjection extends IParametrizable {
	self: ProcessStub[_, _] =>

	def createComposite(baseQuery: List[HCompQuery]): CompositeQuery = CompositeQuery(baseQuery ::: INJECT_QUERIES.get.values.toList,
		"", baseQuery.find(_.title != "").map(_.title).getOrElse(""))

	def createComposite(baseQuery: HCompQuery): CompositeQuery = createComposite(List(baseQuery))

	private def getQueryAnswersFromComposite(compositeAnswer: CompositeQueryAnswer, needle: Map[String, HCompQuery]): Map[String, HCompAnswer] =
		needle.map(q => q._1 -> compositeAnswer.get[HCompAnswer](q._2)).toMap

	def addInjectedAnswersToPatch(patch: Patch, compositeAnswers: List[CompositeQueryAnswer], additionalQueryData: Map[String, HCompQuery] = Map.empty): Unit = {
		val map: List[Map[String, HCompAnswer]] = compositeAnswers.map(c =>
			getQueryAnswersFromComposite(c, INJECT_QUERIES.get) ++
				getQueryAnswersFromComposite(c, additionalQueryData))
		if (map.size > 0) {
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
