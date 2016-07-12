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
object FeatureInfluenceOnIncome_Experiment1 extends App with LazyLogger {
	val features = CSVReader.open("example_data/featuresWithInfluenceOnIncome.csv").all().map(l => Feature(l.head)(l(1)))
	//val features = List(Feature("f1")("have a higher or a lower share of their money made in the agricultural sector (meat/wheat production, farms..) .."), Feature("f2")("spend higher or lower amount of money of their government's budget on research .."))
	val portal = HComp.mechanicalTurk
	U.initDBConnection()

	def getEstimationForFeature(feature: Feature) = {
		val instructions = new TrivialInstructionGenerator("What do you think about the following: In the USA, ",
			"Please estimate the influence of this economic factor", questionAfter = "For example, people who earn more than $50k/yr tend to a lower amount of crimes recorded than people who earn less than $50k/yr. You can accept multiple of these HITs, but please only one per variable")
		val contest = new Contest(Map(PORTAL_PARAMETER.key -> new MySQLDBPortalDecorator(portal, None),
			WORKER_COUNT.key -> 35, OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(instructions),
			QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 5),
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