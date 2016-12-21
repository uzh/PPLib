package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextQuery, HComp}
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}

import scala.util.Random

/**
  * Created by pdeboer on 06/07/16.
  */
object FeatureInfluenceOnSchool_Experiment1 extends App with LazyLogger {
	val portal = new MySQLDBPortalDecorator(HComp.mechanicalTurk)
  HComp.mechanicalTurk.approveAll = false
	U.initDBConnection()

	def runSingleTask(theNumber:Int) = {
		val randomString = Random.alphanumeric.take(10).mkString
		val theUrl = s"http://mbuehler.ch/?name=$randomString"


		val answer = portal.sendQueryAndAwaitResult(FreetextQuery("test")).get

    HComp.mechanicalTurk.rejectAnswer(answer, "asdf") //for rejection
    HComp.mechanicalTurk.approveAndBonusAnswer(answer, "bla") //for approval

    answer
	}

  import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._ //for mpar
  val myListWithAnswers = (1 to 10).mpar.map(runSingleTask)
  print(myListWithAnswers)
}