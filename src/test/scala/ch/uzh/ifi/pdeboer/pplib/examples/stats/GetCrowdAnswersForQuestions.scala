package ch.uzh.ifi.pdeboer.pplib.examples.stats

import java.io.PrintWriter

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompQueryProperties, QuestionRenderer}
import ch.uzh.ifi.pdeboer.pplib.process.entities.{DefaultParameters, ExplicitInstructionGenerator, FileProcessMemoizer, Patch}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Collection
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, StringWrapper}

import scala.xml.NodeSeq

/**
 * Created by pdeboer on 11/06/15.
 */
private[stats] class GetCrowdAnswersForQuestions(data: List[QuestionData]) extends LazyLogger {
	val dataMap: Map[String, QuestionData] = data.zipWithIndex.map(d => d._2.toString -> d._1).toMap

	def getQuestionForIndex(key: String) = dataMap(key)

	def process() {
		logger.info("constructing process")

		val process = new Collection(Map(
			DefaultParameters.PORTAL_PARAMETER.key -> HComp.randomPortal,
			DefaultParameters.WORKER_COUNT.key -> 10,
			DefaultParameters.QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 16),
			DefaultParameters.OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(new ExplicitInstructionGenerator(new StatsQuestionRenderer(dataMap), "Check if 2 given terms in paragraph refer to each other"))
		))

		val memoizer = new FileProcessMemoizer("statsanswers.mem")

		logger.info("processing items..")
		val patches: Iterable[Patch] = dataMap.keys.map(k => new Patch(k, Some(StringWrapper(k))))
		val answers = patches.map(p => {
			val ans = memoizer.mem(s"answersFor_$p")(process.process(p))
			logger.info(s"got answer for ${getQuestionForIndex(p.value)}: $ans")
			ans
		})

		val fullString = answers.map(p => p.map(ans => {
			val q = dataMap(ans.payload.get.asInstanceOf[StringWrapper].toString)
			val a = ans.value
			List(q.method, q.assumption, a).mkString(",")
		})).mkString("\n")

		logger.info("storing output..")

		Some(new PrintWriter("resultsStats.csv")).foreach(p => {
			p.write(fullString)
			p.close()
		})
		logger.info("done")
	}
}

object GetCrowdAnswersForQuestions extends App {
	new GetCrowdAnswersForQuestions(QuestionInput.parse()).process()
}


private[stats] class StatsQuestionRenderer(data: Map[String, ch.uzh.ifi.pdeboer.pplib.examples.stats.QuestionData]) extends QuestionRenderer {
	def getQuestionContents(key: String) = data(key)

	//method color: orange
	//assumption color: yellow
	override def getInstructions(data1: String, data2: String, htmlData: NodeSeq): String = {
		val qData: QuestionData = getQuestionContents(data1)

		<p>Please check, whether the statistical assumption
			<b>
				{qData.assumption}
			</b>
			(highlighted in yellow)
			refers to the statistical method
			<b>
				{qData.method}
			</b>
			(highlighted in orange) within this
			<a href={qData.url}>PDF</a>
			. (i.e. are they related somehow?). You will usually need to scroll to the middle of the
			<a href={qData.url}>PDF</a>
			and need to read part the (short) text between the two highlighted terms. No need to read the rest (except if you’re interested). Your answer will be evaluated by other crowd workers
		</p>.toString
	}
}