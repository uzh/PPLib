package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence.HashValidation.validateToken
import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextQuery, HComp, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}

import scala.util.Random

/**
  * Created by pdeboer on 06/07/16.
  */
object FeatureInfluenceOnSchool_Experiment1 extends App with LazyLogger {
  val answerCount = 5
  val paymentCents = 10
  val dataset_name = s"student"// Adjust dataset name for other datasets

	val portal = new MySQLDBPortalDecorator(HComp.mechanicalTurk)
  HComp.mechanicalTurk.approveAll = false

	U.initDBConnection()

	def runSingleTask(theNumber:Int) = {
    val title = s"Ranking Survey"
    val condition = 1 // layperson
		val randomString = Random.alphanumeric.take(10).mkString
		val theUrl = s"""http://mbuehler.ch/?dataset_name=$dataset_name&amp;condition=$condition&amp;name=$randomString"""
    val queryInstructions = s"""Please go to the url bellow or <a href="$theUrl" target="_blank">click here</a> and solve the given task. After submitting your answer, you will be given a token. Copy the token and paste it here to receive your reward. You are only allowed to complete one of these HITs ("$title") per day.<br/>$theUrl"""
    val rejectedMsg = s"You submitted an invalid token. Please solve the given task and submit the full token displayed after solving the task."
    val approvedMsg = s"Successfully submitted your answer. Thank you for your efforts."

    val properties = HCompQueryProperties(paymentCents = paymentCents)
		val answer = portal.sendQueryAndAwaitResult(FreetextQuery(queryInstructions, "", title), properties = properties).get
    val token = answer.toString() // answer is FreetextQueryAnswer, but we need String for comparison

    val isValid = validateToken(token, randomString)
    println(s"is valid: $isValid")
    if (isValid) {
      HComp.mechanicalTurk.approveAndBonusAnswer(answer, approvedMsg) //for approval
    } else{
      HComp.mechanicalTurk.rejectAnswer(answer, rejectedMsg) //for rejection
    }
	}

  import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._ //for mpar
  val myListWithAnswers = (1 to answerCount).mpar.map(runSingleTask)
  print(myListWithAnswers)
}