package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

import scala.reflect.runtime.universe._
import scala.util.Random
import scala.xml.NodeSeq

/**
  * Created by pdeboer on 28/08/15.
  */
trait HCompQueryBuilder[T] {
	def buildQuery(input: T, base: ProcessStub[_, _], nonBaseClassInstructionGenerator: Option[InstructionGenerator] = None): HCompQuery

	def parseAnswer[TARGET](input: T, answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: TypeTag[TARGET]): Option[TARGET]

	protected def prepareInstructions(base: ProcessStub[_, _], nonBaseClassInstructionGenerator: Option[InstructionGenerator]): (QuestionRenderer, String) = {
		if (nonBaseClassInstructionGenerator.isDefined) {
			val ig: InstructionGenerator = nonBaseClassInstructionGenerator.get
			val instructionData: InstructionData = base.getParam(INSTRUCTIONS)
			(ig.generateQuestion(instructionData), ig.generateQuestionTitle(instructionData))
		} else {
			base match {
				case ih: InstructionHandler =>
					val instructionTitle = ih.instructionTitle + " [" + Math.abs(Random.nextDouble()) + "]"

					(ih.instructions, instructionTitle)
				case _ => throw new IllegalStateException("We only support Instruction-Handling enabled processes")
			}
		}
	}
}

import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

class DefaultMCQueryBuilder(maxAnswers: Int = 1, minAnswers: Int = 1, italicInstructionsParam: ProcessParameter[String] = INSTRUCTIONS_ITALIC,
							auxParam: ProcessParameter[Option[NodeSeq]] = QUESTION_AUX, shuffleChoicesParam: ProcessParameter[Boolean] = SHUFFLE_CHOICES) extends HCompQueryBuilder[List[Patch]] {

	override def buildQuery(input: List[Patch], base: ProcessStub[_, _], nonBaseClassInstructionGenerator: Option[InstructionGenerator]): HCompQuery = {
		val instructionItalic: String = base.getParamOption(italicInstructionsParam).getOrElse("")
		val htmlData: NodeSeq = base.getParamOption(auxParam).getOrElse(Some(Nil)).getOrElse(Nil)
		val choices = if (base.getParamOption(shuffleChoicesParam).getOrElse(true)) Random.shuffle(input) else input

		val (instructions, instructionTitle) = prepareInstructions(base, nonBaseClassInstructionGenerator)

		MultipleChoiceQuery(instructions.getInstructions(instructionItalic, htmlData = htmlData), choices.map(_.value), maxAnswers, minAnswers, instructionTitle)
	}

	override def parseAnswer[TARGET](input: List[Patch], answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: TypeTag[TARGET]): Option[TARGET] = {
		val castedAnswer = answer.is[MultipleChoiceAnswer]
		val tpe = baseCls.tpe
		val ret = if (tpe <:< typeOf[String]) {
			assert(maxAnswers == 1)
			Some(castedAnswer.selectedAnswer)
		} else if (tpe <:< typeOf[Patch]) {
			assert(maxAnswers == 1)
			Some(input.find(i => i.value == castedAnswer.selectedAnswer).get)
		} else if (tpe <:< typeOf[List[String]]) Some(castedAnswer.selectedAnswers)
		else None

		ret.asInstanceOf[Option[TARGET]] //this is ugly. but at least we got some magic above
	}
}

class DefaultTextQueryBuilder(italicInstructionsParam: ProcessParameter[String] = INSTRUCTIONS_ITALIC,
							  auxParam: ProcessParameter[Option[NodeSeq]] = QUESTION_AUX) extends HCompQueryBuilder[Patch] {

	override def buildQuery(input: Patch, base: ProcessStub[_, _], nonBaseClassInstructionGenerator: Option[InstructionGenerator]): HCompQuery = {
		val instructionItalic: String = base.getParamOption(italicInstructionsParam).getOrElse("")
		val htmlData: NodeSeq = base.getParamOption(auxParam).getOrElse(Some(Nil)).getOrElse(Nil)

		val (instructions, instructionTitle) = prepareInstructions(base, nonBaseClassInstructionGenerator)

		FreetextQuery(instructions.getInstructions(input.value, instructionItalic, htmlData), "", instructionTitle)
	}

	override def parseAnswer[TARGET](input: Patch, answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: TypeTag[TARGET]): Option[TARGET] = {
		Some(answer.is[FreetextAnswer].answer).asInstanceOf[Option[TARGET]]
	}
}


class DefaultPercentageQueryBuilder(italicInstructionsParam: ProcessParameter[String] = INSTRUCTIONS_ITALIC,
									auxParam: ProcessParameter[Option[NodeSeq]] = QUESTION_AUX) extends HCompQueryBuilder[Patch] {

	override def buildQuery(input: Patch, base: ProcessStub[_, _], nonBaseClassInstructionGenerator: Option[InstructionGenerator]): HCompQuery = {
		val instructionItalic: String = base.getParamOption(italicInstructionsParam).getOrElse("")
		val htmlData: NodeSeq = base.getParamOption(auxParam).getOrElse(Some(Nil)).getOrElse(<i>Please enter a percentage followed by the '%' sign. For example: 59%. Please use only integer percentages (e.g. 23.5% is not valid, 23% is). Your number should be between 0% and 100%. Your estimates for all of these estimation questions must sum up to 100% in order for you to get paid.</i>)

		val (instructions, instructionTitle) = prepareInstructions(base, nonBaseClassInstructionGenerator)

		FreetextQuery(instructions.getInstructions(input.value, instructionItalic, htmlData), "", instructionTitle)
	}

	override def parseAnswer[TARGET](input: Patch, answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: TypeTag[TARGET]): Option[TARGET] = {
		val textAnswer = answer.is[FreetextAnswer].answer
		val doubleAnswer = textAnswer.replaceAll("[^0-9\\.]", "").toDouble
		val percentage = if (doubleAnswer > 1) doubleAnswer / 100d else doubleAnswer
		Some(percentage).asInstanceOf[Option[TARGET]]
	}
}


trait HCompQueryBuilderSupport[INPUT] extends IParametrizable {
	self: ProcessStub[INPUT, _] =>

	val queryBuilderParam = defaultParameters.find(_.key == DefaultParameters.QUERY_BUILDER_KEY).get.asInstanceOf[ProcessParameter[HCompQueryBuilder[INPUT]]]

	def queryBuilder = self.getParam(queryBuilderParam)

	override def defaultParameters: List[ProcessParameter[_]] = combineParameterLists(List(DefaultParameters.newQueryBuilderParam(inputClass, inputType)), super.defaultParameters)
}