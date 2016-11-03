package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, MultipleChoiceQuery}
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, TrivialInstructionGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Contest
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}
import com.github.tototoshi.csv.CSVReader

import scala.util.Random

/**
  * Created by pdeboer on 06/07/16.
  */
object FeatureInfluenceOnOlympia_Experiment2 extends App with LazyLogger {
	val features = CSVReader.open("example_data/featuresOlympia_hi_lo.csv").all().map(l => Feature(l.head)(l(1)))
	val featureGroups = features.groupBy(_.name.take(6)).values.toList
	//val features = List(Feature("f1")("have a higher or a lower share of their money made in the agricultural sector (meat/wheat production, farms..) .."), Feature("f2")("spend higher or lower amount of money of their government's budget on research .."))
	val portal = HComp.mechanicalTurk
	U.initDBConnection()

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

	def getEstimationForFeatureGroup(features: List[Feature]) = {
		val instructions = new TrivialInstructionGenerator("What do you think about the following:  ",
			"How well can you predict the olympics?", questionAfter = "Please only accept one of these HITs per day") //You can accept multiple of these HITs, but please only *one per topic* (topics marked with the asterisks **).
		val choices: List[String] = (1 to 10).map(x => s"$x (${x}0%)").toList
		val contest = new Contest(Map(PORTAL_PARAMETER.key -> new MySQLDBPortalDecorator(portal, None),
			WORKER_COUNT.key -> 20, OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(instructions),
			INSTRUCTIONS_ITALIC.key -> features.head.description,
			INJECT_QUERIES.key -> features.drop(1).map(f => f.name -> MultipleChoiceQuery(f.description, Random.shuffle(choices), 1)).toMap
		))
		val res = contest.process(IndexedPatch.from(choices))
		val votes = contest.extractContestResultFromWinner(res)
		logger.info(s"Feature $features received vote $votes")
		votes
	}

	import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

	featureGroups.mpar.foreach(getEstimationForFeatureGroup)
	println("done")
}