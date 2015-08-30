package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.hcomp.{QuestionRenderer, StringQuestionRenderer}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Created by pdeboer on 28/11/14.
 */
@SerialVersionUID(1l) class ProcessParameter[T](val keyPostfix: String, val candidateDefinitions: Option[Iterable[T]] = None)(implicit baseClass: ClassTag[T], val baseType: TypeTag[T]) extends Serializable {
	def key = keyPostfix

	def clazz: Class[_ <: ProcessStub[_, _]] = baseClass.runtimeClass.asInstanceOf[Class[ProcessStub[_, _]]]

	def get(implicit processStub: ProcessStub[_, _]) = processStub.getParam(this)

	override def toString: String = key


	def canEqual(other: Any): Boolean = other.isInstanceOf[ProcessParameter[_]]

	override def equals(other: Any): Boolean = other match {
		case that: ProcessParameter[_] =>
			(that canEqual this) &&
				keyPostfix == that.keyPostfix
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(keyPostfix)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}

trait InstructionGenerator {
	def generateQuestion(base: InstructionData): QuestionRenderer

	def generateQuestionTitle(base: InstructionData): String
}

class InstructionData(
						 val objectName: String = "paragraph",
						 val actionName: String = "refine the following paragraph",
						 val detailedDescription: String = "grammar (e.g. tenses), coherence and text-sophistication",
						 val evaluation: String = "Malicious/unchanged answers will get rejected. Your answer will be evaluated by other crowd workers.") {


	def canEqual(other: Any): Boolean = other.isInstanceOf[InstructionData]

	override def equals(other: Any): Boolean = other match {
		case that: InstructionData =>
			(that canEqual this) &&
				objectName == that.objectName &&
				actionName == that.actionName &&
				detailedDescription == that.detailedDescription &&
				evaluation == that.evaluation
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(objectName, actionName, detailedDescription, evaluation)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}

class ExplicitInstructionGenerator(question: QuestionRenderer, title: String) extends InstructionGenerator {
	override def generateQuestion(base: InstructionData): QuestionRenderer = question

	override def generateQuestionTitle(base: InstructionData): String = title
}

class SimpleInstructionGeneratorCreate extends InstructionGenerator {
	override def generateQuestion(base: InstructionData): QuestionRenderer = new StringQuestionRenderer(
		generateQuestionTitle(base) + ". Please pay special attention to " + base.detailedDescription, base.evaluation)

	override def generateQuestionTitle(base: InstructionData): String = "Please " + base.actionName
}

class SimpleInstructionGeneratorDecide extends InstructionGenerator {
	override def generateQuestion(base: InstructionData): QuestionRenderer = new StringQuestionRenderer(
		"Before, crowd workers were asked to " + base.actionName, "Please select the answer you like best in terms of " + base.detailedDescription
	)

	override def generateQuestionTitle(base: InstructionData): String = s"Please select the ${base.objectName} you like best"
}