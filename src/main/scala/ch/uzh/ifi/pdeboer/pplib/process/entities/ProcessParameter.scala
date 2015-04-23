package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTuple, HCompInstructionsWithTupleStringified}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Created by pdeboer on 28/11/14.
 */
@SerialVersionUID(1l) class ProcessParameter[T](keyPostfix: String, val candidateDefinitions: Option[Iterable[T]] = None)(implicit baseClass: ClassTag[T], val baseType: TypeTag[T]) extends Serializable {
	def key = keyPostfix

	def clazz: Class[_ <: ProcessStub[_, _]] = baseClass.runtimeClass.asInstanceOf[Class[ProcessStub[_, _]]]

	def get(implicit processStub: ProcessStub[_, _]) = processStub.getParam(this)

	override def toString: String = key
}

trait InstructionGenerator {
	def generateQuestion(base: InstructionData): HCompInstructionsWithTuple

	def generateQuestionTitle(base: InstructionData): String
}

class InstructionData(
						 val objectName: String = "paragraph",
						 val actionName: String = "refine the following paragraph",
						 val detailedDescription: String = "grammar (e.g. tenses), coherence and text-sophistication",
						 val evaluation: String = "Malicious/unchanged answers will get rejected. Your answer will be evaluated by other crowd workers.") {
}

class ExplicitInstructionGenerator(question: HCompInstructionsWithTuple, title: String) extends InstructionGenerator {
	override def generateQuestion(base: InstructionData): HCompInstructionsWithTuple = question

	override def generateQuestionTitle(base: InstructionData): String = title
}

class SimpleInstructionGeneratorCreate extends InstructionGenerator {
	override def generateQuestion(base: InstructionData): HCompInstructionsWithTuple = new HCompInstructionsWithTupleStringified(
		generateQuestionTitle(base) + "in terms of " + base.detailedDescription, base.evaluation)

	override def generateQuestionTitle(base: InstructionData): String = "Please " + base.actionName
}

class SimpleInstructionGeneratorDecide extends InstructionGenerator {
	override def generateQuestion(base: InstructionData): HCompInstructionsWithTuple = new HCompInstructionsWithTupleStringified(
		"Before, crowd workers were asked to " + base.actionName, "Please select the one you like best in terms of " + base.detailedDescription
	)

	override def generateQuestionTitle(base: InstructionData): String = s"Please select the ${base.objectName} you like best"
}