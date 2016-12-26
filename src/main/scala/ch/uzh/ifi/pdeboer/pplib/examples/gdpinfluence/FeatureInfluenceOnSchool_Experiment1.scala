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
		val theUrl = s"http://http://mbuehler.ch/?dataset_name=student&condition=1&name=$randomString"
    val queryInstructions = s"Please go to the url bellow and solve the given task. After submitting your answer, you will be given a token. Copy the token and paste it here to receive your reward. URL: $theUrl"
    val rejectedMsg = "You submitted an invalid token. Please solve the given task and submit the full token displayed after solving the task."
    val approvedMsg = "Successfully submitted your answer. Thank you for your efforts."

		val answer = portal.sendQueryAndAwaitResult(FreetextQuery(queryInstructions)).get

    HComp.mechanicalTurk.rejectAnswer(answer, rejectedMsg) //for rejection
    HComp.mechanicalTurk.approveAndBonusAnswer(answer, approvedMsg) //for approval

    answer
	}

  import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._ //for mpar
  val myListWithAnswers = (1 to 10).mpar.map(runSingleTask)
  print(myListWithAnswers)
}