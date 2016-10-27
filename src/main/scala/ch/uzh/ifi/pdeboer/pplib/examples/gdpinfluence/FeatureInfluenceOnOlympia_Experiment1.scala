package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, TrivialInstructionGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Contest
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}
import com.github.tototoshi.csv.CSVReader

/**
  * Created by pdeboer on 06/07/16.
  */
object FeatureInfluenceOnOlympia_Experiment1 extends App with LazyLogger {
	val features = CSVReader.open("example_data/featuresOlympia.csv").all().map(l => Feature(l.head)(l(1)))
	//val features = List(Feature("f1")("have a higher or a lower share of their money made in the agricultural sector (meat/wheat production, farms..) .."), Feature("f2")("spend higher or lower amount of money of their government's budget on research .."))
	val portal = HComp.mechanicalTurk
	U.initDBConnection()

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

	def getEstimationForFeature(feature: Feature) = {
		val instructions = new TrivialInstructionGenerator("What do you think about the following: In the USA, ",
			"How well can you predict the olympics?", questionAfter = "Your answer must be a number. Characters are NOT allowed. Please only accept one of these HITs per day. ") //You can accept multiple of these HITs, but please only *one per topic* (topics marked with the asterisks **).
		val contest = new Contest(Map(PORTAL_PARAMETER.key -> new MySQLDBPortalDecorator(portal, None),
			WORKER_COUNT.key -> 20, OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(instructions),
			QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 5),
			INSTRUCTIONS_ITALIC.key -> feature.description))
		val choices: List[String] = (1 to 10).map(x => s"$x (${x}0%)").toList
		val res = contest.process(IndexedPatch.from(choices))
		val votes = contest.extractContestResultFromWinner(res)
		logger.info(s"Feature $feature received vote $votes")
		votes
	}

	import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

	features.mpar.foreach(getEstimationForFeature)
	println("done")
}