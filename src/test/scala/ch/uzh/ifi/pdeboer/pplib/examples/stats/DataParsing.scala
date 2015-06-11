package ch.uzh.ifi.pdeboer.pplib.examples.stats

import scala.io.Source

/**
 * Created by pdeboer on 11/06/15.
 */
private[stats] object QuestionInput {
	def parse(): List[QuestionData] = Source.fromFile("exampleQuestionData.csv").getLines().drop(1).map(l => {
		//example line: t-test,normality,http://dropbox.com/mike.pdf
		val cols = l.split(",")
		QuestionData(cols(0), cols(1), cols(2))
	}).toList
}

private[stats] case class QuestionData(method: String, assumption: String, url: String)