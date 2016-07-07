package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextQuery, HComp, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, TrivialInstructionGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Contest
import ch.uzh.ifi.pdeboer.pplib.util.{CollectionUtils, LazyLogger, U}
import com.github.tototoshi.csv.CSVReader

/**
  * Created by pdeboer on 06/07/16.
  */
object FeatureInfluenceOnGDP_Experiment2 extends App with LazyLogger {
	val features = CSVReader.open("example_data/featuresWithInfluenceOnGDP.csv").all().map(l => Feature(l.head)(l(2)))
	//val features = List(Feature("f1")("have a higher or a lower share of their money made in the agricultural sector (meat/wheat production, farms..) .."), Feature("f2")("spend higher or lower amount of money of their government's budget on research .."))
	val portal = HComp.mechanicalTurk
	U.initDBConnection()

	def getEstimationForFeature(feature: Feature) = {
		val instructions = new TrivialInstructionGenerator("Do countries with high average income per person ...",
			"Please estimate the influence of this economic factor", questionAfter = ".. than countries with low average income pP. Example: countries with high average income tend to have lower crime rates than countries with lower average income. Feel free to take multiple of these HITS, but only answer each influencing factor once. ")
		val contest = new Contest(Map(PORTAL_PARAMETER.key -> new MySQLDBPortalDecorator(portal, None),
			WORKER_COUNT.key -> 31, OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(instructions),
			QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 4),
			INJECT_QUERIES.key -> Map("why" -> FreetextQuery("why do you think so?")),
			INSTRUCTIONS_ITALIC.key -> feature.description))
		val choices: List[String] = List("higher", "lower")
		val res = contest.process(IndexedPatch.from(choices))
		val votes = contest.extractContestResultFromWinner(res)
		logger.info(s"Feature $feature received vote $votes")
		votes
	}

	import CollectionUtils._

	features.mpar.foreach(getEstimationForFeature)
}