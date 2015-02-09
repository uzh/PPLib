package ch.uzh.ifi.pdeboer.pplib.process.parameter

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTuple, HCompInstructionsWithTupleStringified}
import ch.uzh.ifi.pdeboer.pplib.process.ProcessStub

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 28/11/14.
 */
@SerialVersionUID(1l) class ProcessParameter[T: ClassTag](keyPostfix: String, val candidateDefinitions: Option[Iterable[T]] = None) extends Serializable {
	def key = keyPostfix

	def clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass

	def t = implicitly[ClassTag[T]]

	def get(implicit processStub: ProcessStub[_, _]) = processStub.getParam(this)

	override def toString: String = key
}

trait InstructionGenerator {
	def generate(base: InstructionData): HCompInstructionsWithTuple
}

class InstructionData(val actionName: String = "refine the following paragraph",
					  val detailedDescription: String = "grammar (e.g. tenses), coherence and text-sophistication",
					  val evaluation: String = "Malicious/unchanged answers will get rejected. Your answer will be evaluated by other crowd workers.") {
}

class SimpleInstructionGeneratorCreate extends InstructionGenerator {
	override def generate(base: InstructionData): HCompInstructionsWithTuple = new HCompInstructionsWithTupleStringified(
		"Please " + base.actionName + "in terms of " + base.detailedDescription, base.evaluation)
}

class SimpleInstructionGeneratorDecide extends InstructionGenerator {
	override def generate(base: InstructionData): HCompInstructionsWithTuple = new HCompInstructionsWithTupleStringified(
		"Before, crowd workers were asked to " + base.actionName, "Please select the one you like best in terms of " + base.detailedDescription
	)
}