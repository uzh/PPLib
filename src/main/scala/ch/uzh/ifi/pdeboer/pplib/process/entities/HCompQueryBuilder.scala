package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompQuery, MultipleChoiceAnswer, MultipleChoiceQuery}

import scala.reflect.ClassTag
import scala.util.Random
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 28/08/15.
 */
trait HCompQueryBuilder[T] {
	def buildQuery(queryKey: String, input: T, base: ProcessStub[_, _]): HCompQuery

	def parseAnswer[TARGET](queryKey: String, input: T, answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: ClassTag[TARGET]): Option[TARGET]
}

class DefaultMCQueryBuilder(maxAnswers: Int = 1, minAnswers: Int = 1) extends HCompQueryBuilder[List[Patch]] {

	import DefaultParameters._

	override def buildQuery(queryKey: String, input: List[Patch], base: ProcessStub[_, _]): HCompQuery = {
		base match {
			case ih: InstructionHandler =>
				val instructionItalic: String = base.getParamOption(INSTRUCTIONS_ITALIC).getOrElse("")
				val htmlData: NodeSeq = base.getParamOption(QUESTION_AUX).getOrElse(Some(Nil)).getOrElse(Nil)
				val instructions: String = ih.instructions.getInstructions(instructionItalic, htmlData = htmlData)
				val choices = if (base.getParamOption(SHUFFLE_CHOICES).getOrElse(true)) Random.shuffle(input) else input

				MultipleChoiceQuery(instructions, choices.map(_.value), maxAnswers, minAnswers, ih.instructionTitle + " [" + Math.abs(Random.nextDouble()) + "]")
			case _ => throw new IllegalStateException("Default MC Query is only supported for Instruction-Handling enabled processes")
		}
	}

	override def parseAnswer[TARGET](queryKey: String, input: List[Patch], answer: HCompAnswer, base: ProcessStub[_, _])(implicit baseCls: ClassTag[TARGET]): Option[TARGET] = {
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

trait HCompQueryBuilderSupport[INPUT] extends IParametrizable {
	self: ProcessStub[INPUT, _] =>

	val queryBuilderParam = defaultParameters.find(_.key == DefaultParameters.QUERY_BUILDER_KEY).get.asInstanceOf[ProcessParameter[HCompQueryBuilder[INPUT]]]

	def queryBuilder = self.getParam(queryBuilderParam)

	override def defaultParameters: List[ProcessParameter[_]] = combineParameterLists(List(DefaultParameters.newQueryBuilderParam(inputClass, inputType)), super.defaultParameters)
}