package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextQuery, HComp, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._
import ch.uzh.ifi.pdeboer.pplib.process.entities.{Patch, TrivialInstructionGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Collection
import ch.uzh.ifi.pdeboer.pplib.util.{CollectionUtils, LazyLogger, U}
import com.github.tototoshi.csv.CSVReader

/**
  * Created by pdeboer on 06/07/16.
  */
object FeatureInfluenceOnOlympia_Experiment1 extends App with LazyLogger {
	val features = CSVReader.open("example_data/featuresOlympia.csv").all().map(l => Feature(l.head)(l(1)))
	//val features = List(Feature("f1")("have a higher or a lower share of their money made in the agricultural sector (meat/wheat production, farms..) .."), Feature("f2")("spend higher or lower amount of money of their government's budget on research .."))
	val portal = HComp.mechanicalTurk
	U.initDBConnection()

	val decoratedPortal = new MySQLDBPortalDecorator(portal, None)


	def getEstimationForFeatureThroughCollection(feature: Feature) = {
		val instructions = new TrivialInstructionGenerator("What do you think about the following: " + feature.description,
			"Please estimate the influence of this factor", questionAfter = "Your answer must be a number. Characters are NOT allowed. You can accept multiple of these HITs, but please only *one per variable* (marked with the asterisks **). ")
		val contest = new Collection(Map(PORTAL_PARAMETER.key -> new MySQLDBPortalDecorator(portal, None),
			WORKER_COUNT.key -> 1, OVERRIDE_INSTRUCTION_GENERATOR.key -> Some(instructions),
			QUESTION_PRICE.key -> HCompQueryProperties(paymentCents = 5),
			INJECT_QUERIES.key -> Map("why" -> FreetextQuery("why do you think so?")),
			INSTRUCTIONS_ITALIC.key -> feature.description))
		val res = contest.process(new Patch(""))
		res
	}

	import CollectionUtils._

	features.mpar.foreach(getEstimationForFeatureThroughCollection)
}