package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.hcomp._

import scala.reflect.ClassTag
import scala.util.Random
import scala.xml.NodeSeq

/**
  * Created by pdeboer on 28/08/15.
  */
trait HCompQueryBuilder[T] {
	def buildQuery(input: T, base: ProcessStub[_, _], payload: Any = ""): HCompQuery

	def parseAnswer[TARGET](input: T, answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: ClassTag[TARGET]): Option[TARGET]
}

import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

class DefaultMCQueryBuilder(maxAnswers: Int = 1, minAnswers: Int = 1, italicInstructionsParam: ProcessParameter[String] = INSTRUCTIONS_ITALIC,
							auxParam: ProcessParameter[Option[NodeSeq]] = QUESTION_AUX, shuffleChoicesParam: ProcessParameter[Boolean] = SHUFFLE_CHOICES) extends HCompQueryBuilder[List[Patch]] {

	override def buildQuery(input: List[Patch], base: ProcessStub[_, _], payload: Any = ""): HCompQuery = {
		base match {
			case ih: InstructionHandler =>
				val instructionItalic: String = base.getParamOption(italicInstructionsParam).getOrElse("")
				val htmlData: NodeSeq = base.getParamOption(auxParam).getOrElse(Some(Nil)).getOrElse(Nil)
				val instructions: String = ih.instructions.getInstructions(instructionItalic, htmlData = htmlData)
				val choices = if (base.getParamOption(shuffleChoicesParam).getOrElse(true)) Random.shuffle(input) else input

				MultipleChoiceQuery(instructions, choices.map(_.value), maxAnswers, minAnswers, ih.instructionTitle + " [" + Math.abs(Random.nextDouble()) + "]")
			case _ => throw new IllegalStateException("Default MC Query is only supported for Instruction-Handling enabled processes")
		}
	}

	override def parseAnswer[TARGET](input: List[Patch], answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: ClassTag[TARGET]): Option[TARGET] = {
		val castedAnswer = answer.is[MultipleChoiceAnswer]
		val ret = baseCls.runtimeClass match {
			case s: Class[String] =>
				assert(maxAnswers == 1)
				Some(castedAnswer.selectedAnswer)
			case li: Class[List[String]] =>
				Some(castedAnswer.selectedAnswers)
			case _ => None
		}
		ret.asInstanceOf[Option[TARGET]] //this is ugly. but at least we got some magic above
	}
}

class DefaultTextQueryBuilder(italicInstructionsParam: ProcessParameter[String] = INSTRUCTIONS_ITALIC,
							  auxParam: ProcessParameter[Option[NodeSeq]] = QUESTION_AUX) extends HCompQueryBuilder[Patch] {

	override def buildQuery(input: Patch, base: ProcessStub[_, _], payload: Any): HCompQuery = base match {
		case ih: InstructionHandler =>
			val instructionItalic: String = base.getParamOption(italicInstructionsParam).getOrElse("")
			val htmlData: NodeSeq = base.getParamOption(auxParam).getOrElse(Some(Nil)).getOrElse(Nil)
			val instructions: String = ih.instructions.getInstructions(instructionItalic, htmlData = htmlData)

			FreetextQuery(instructions, "", ih.instructionTitle + " [" + Math.abs(Random.nextDouble()) + "]")
		case _ => throw new IllegalStateException("Default Text Query is only supported for Instruction-Handling enabled processes")
	}

	override def parseAnswer[TARGET](input: Patch, answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: ClassTag[TARGET]): Option[TARGET] = {
		Some(answer.is[FreetextAnswer].answer).asInstanceOf[Option[TARGET]]
	}
}

trait HCompQueryBuilderSupport[INPUT] extends IParametrizable {
	self: ProcessStub[INPUT, _] =>

	val queryBuilderParam = defaultParameters.find(_.key == DefaultParameters.QUERY_BUILDER_KEY).get.asInstanceOf[ProcessParameter[HCompQueryBuilder[INPUT]]]

	def queryBuilder = self.getParam(queryBuilderParam)

	override def defaultParameters: List[ProcessParameter[_]] = combineParameterLists(List(DefaultParameters.newQueryBuilderParam(inputClass, inputType)), super.defaultParameters)
}