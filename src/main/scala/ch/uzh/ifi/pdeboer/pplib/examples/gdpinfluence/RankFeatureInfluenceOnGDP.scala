package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextQuery, HComp, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, TrivialInstructionGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Contest
import ch.uzh.ifi.pdeboer.pplib.util.U
import com.github.tototoshi.csv.CSVReader

/**
  * Created by pdeboer on 06/07/16.
  */
object RankFeatureInfluenceOnGDP extends App {
	val features = CSVReader.open("example_data/featuresWithInfluenceOnGDP.csv").all().map(l => Feature(l.head)(l(1)))
	//val features = List(Feature("f1")("Percentage of the GDP made in the agricultural sector (meat/wheat production, farms..)"), Feature("f2")("Percentage of government's budget spent on research"))
	val portal = HComp.mechanicalTurk
	U.initDBConnection()

	def compareTuple(t1: Feature, t2: Feature, index: Int = 0) = {
		if (t1 == null) t2
		else if (t2 == null) t1
		else {
			val instructions = new TrivialInstructionGenerator("Which of the two factors below do you think influences the average income of a country's people more.",
				"Please rank these two influencing factors", questionAfter = "Please note, that we are NOT trying to find out which of the two factor's influences is more positive than the other. We are trying to see which factor is more important.")
			val contest = new Contest(Map(PORTAL_PARAMETER.key -> new MySQLDBPortalDecorator(portal, Some(index)), WORKER_COUNT.key -> 1, OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(instructions), QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 10, qualifications = Nil), INJECT_QUERIES.key -> Map("why" -> FreetextQuery("why do you think so?"), "clear" -> FreetextQuery("We are planning on launching a large batch of such hits. Was this HIT clear to you? How could it be improved?")), INSTRUCTIONS_ITALIC.key -> "Example: a country's crime rate has a higher impact on the average income per person than the amount of trees in said country."))
			val res = contest.process(IndexedPatch.from(List(t1.description, t2.description)))
			def featureByDescription(key: String) = List(t1, t2).find(_.description == key).get
			val votes = contest.extractContestResultFromWinner(res).map(k => featureByDescription(k._1.get) -> k._2)
			val winnerFeature: Feature = featureByDescription(res.value)
			println(s"$winnerFeature wins in competition between $t1 [ ${votes.getOrElse(t1, 0)} ] and $t2 [ ${votes.getOrElse(t2, 0)} ]")
			List(t1, t2).foreach(l => l.votes += votes.getOrElse(l, 0))
			winnerFeature
		}
	}

	val featureTuples = features.flatMap(f1 => {
		features.map(f2 => {
			if (f1 != f2)
				Set(f1, f2)
			else Set.empty[Feature]
		})
	}).toSet
	println(featureTuples.zipWithIndex.mkString("\n"))

	featureTuples.toList.zipWithIndex.foreach(ti => {
		val t = ti._1
		if (t.size == 2) {
			val l: List[Feature] = t.toList
			val (f1, f2) = (l.head, l(1))
			compareTuple(f1, f2, ti._2)
		}
	})
	features.sortBy(-_.votes).map(f => s"${f.name} : ${f.votes}").foreach(println)

	def findWinner(features: List[Feature]): Feature = {
		if (features.isEmpty) null
		else if (features.length == 1) features.head
		else if (features.size == 2) compareTuple(features.head, features(1))
		else {
			val splitIndex = features.size / 2
			val left = features.take(splitIndex)
			val right = features.drop(splitIndex)

			val (leftWinner, rightWinner) = (findWinner(left), findWinner(right))
			compareTuple(leftWinner, rightWinner)
		}
	}
}

private[examples] case class Feature(name: String)(val description: String, var votes: Int = 0)
