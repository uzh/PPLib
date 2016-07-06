package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import ch.uzh.ifi.pdeboer.pplib.process.entities.{DefaultParameters, IndexedPatch, TrivialInstructionGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Contest
import com.github.tototoshi.csv.CSVReader
import DefaultParameters._

/**
  * Created by pdeboer on 06/07/16.
  */
object RankFeatureInfluenceOnGDP extends App {
	val features = CSVReader.open("example_data/featuresWithInfluenceOnGDP.csv").all().map(l => Feature(l.head)(l(1)))

	val portal = HComp.randomPortal

	def compareTuple(t1: Feature, t2: Feature) = {
		if (t1 == null) t2
		else if (t2 == null) t1
		else {
			val instructions = new TrivialInstructionGenerator("Which of the two factors below do you think influences the average income of a country's people more.",
				"Please rank these two influencing factors", questionAfter = "Please note, that we are NOT trying to find out which of the two factor's influences is more positive than the other. We are trying to see which factor is more important.")
			val contest = new Contest(Map(PORTAL_PARAMETER.key -> portal, WORKER_COUNT.key -> 25, OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(instructions), INSTRUCTIONS_ITALIC.key -> "Example: a countries crime rate has a higher impact on the average income per person than the amount of trees in said country."))
			val res = contest.process(IndexedPatch.from(List(t1.description, t2.description)))
			def featureByKey(key: String) = List(t1, t2).find(_.description == key).get
			val votes = contest.extractContestResultFromWinner(res).map(k => featureByKey(k._1.get) -> k._2)
			val winnerFeature: Feature = featureByKey(res.value)
			println(s"$winnerFeature wins in competition between $t1 [ ${votes(t1)} ] and $t2 [ ${votes(t2)} ]")
			winnerFeature
		}
	}

	def recursiveCompare(features: List[Feature]): Feature = {
		if (features.isEmpty) null
		else if (features.length == 1) features.head
		else if (features.size == 2) compareTuple(features.head, features(1))
		else {
			val splitIndex = features.size / 2
			val left = features.take(splitIndex)
			val right = features.drop(splitIndex)

			val (leftWinner, rightWinner) = (recursiveCompare(left), recursiveCompare(right))
			compareTuple(leftWinner, rightWinner)
		}
	}

	val winner = recursiveCompare(features)
	println(s"winner: $winner")
}

private[examples] case class Feature(name: String)(val description: String)
