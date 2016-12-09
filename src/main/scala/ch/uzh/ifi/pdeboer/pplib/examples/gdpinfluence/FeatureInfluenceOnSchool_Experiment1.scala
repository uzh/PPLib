package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, MultipleChoiceQuery, RejectMultiAnswerHCompPortal}
import ch.uzh.ifi.pdeboer.pplib.process.entities.{IndexedPatch, TrivialInstructionGenerator}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.Contest
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}
import com.github.tototoshi.csv.CSVReader

import scala.util.Random

/**
  * Created by pdeboer on 06/07/16.
  */
object FeatureInfluenceOnSchool_Experiment1 extends App with LazyLogger {
/*	val portal = HComp.mechanicalTurk
	portal.approveAll = false
	U.initDBConnection()

	def runSingleTask(): Unit = {
		val randomString = Random.alphanumeric.take(10).mkString
		val theUrl = s"http://mbuehler.ch/?name=$randomString"

		//FreetextQuery("test")
		portal.sendQueryAndAwaitResult()
	}

	import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

	featureGroups.mpar.foreach(getEstimationForFeatureGroup)
	println("done") */
}