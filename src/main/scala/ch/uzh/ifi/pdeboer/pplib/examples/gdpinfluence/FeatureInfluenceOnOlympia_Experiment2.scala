package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompQueryProperties, MultipleChoiceQuery, RejectMultiAnswerHCompPortal}
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, TrivialInstructionGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Contest
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}
import com.github.tototoshi.csv.CSVReader

import scala.util.Random
/**
  * Created by pdeboer on 06/07/16.
  */
object FeatureInfluenceOnOlympia_Experiment2 extends App with LazyLogger {
	def featureGroup(s: String): String = if (s.endsWith("_0") || s.endsWith("_1")) s.substring(0, s.length - 2) else s

	val features = CSVReader.open("example_data/featuresIncome.csv").all().map(l => Feature(l.head)(l(1)))
	val featureGroups = features.groupBy(f => featureGroup(f.name)).values.toList
	//val features = List(Feature("f1")("have a higher or a lower share of their money made in the agricultural sector (meat/wheat production, farms..) .."), Feature("f2")("spend higher or lower amount of money of their government's budget on research .."))
	val portal = HComp.mechanicalTurk
	portal.approveAll = false
	U.initDBConnection()
  val decoratedPortal = new MySQLDBPortalDecorator(new RejectMultiAnswerHCompPortal(portal), None)


	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
	def getEstimationForFeatureGroup(features: List[Feature]) = {
		val instructions = new TrivialInstructionGenerator("What do you think about the following:  ",
			"How well can you predict people's income?", questionAfter = "Please do not accept more than one of my HITs per 24h. We will only approve one answer per worker (per day).")
		val choices: List[String] = (1 to 10).map(x => s"$x (${x}0%)").toList
    val contest = new Contest(Map(PORTAL_PARAMETER.key -> decoratedPortal,
      WORKER_COUNT.key -> 1, OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(instructions),
			QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 10),
			INSTRUCTIONS_ITALIC.key -> features.head.description,
			INJECT_QUERIES.key -> features.drop(1).map(f => f.name -> MultipleChoiceQuery(f.description, Random.shuffle(choices), 1)).toMap
		))
		val res = contest.process(IndexedPatch.from(choices))
		val votes = contest.extractContestResultFromWinner(res)
		votes
	}

	import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

	featureGroups.mpar.foreach(getEstimationForFeatureGroup)
	println("done")
}