package ch.uzh.ifi.pdeboer.pplib.examples.singlequestion

import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextQuery, HComp, HCompQueryProperties}

import scala.concurrent.duration._

/**
 * Created by pdeboer on 26/06/15.
 */
object SingleQuestionExample extends App {
	val links = List(
		"https://uozdoe.qualtrics.com/SE/?SID=SV_7V9P7lP1xPXhQkB"
	)
	links.zipWithIndex.par.foreach(li => {
		try {
			val l = li._1
			println("Asking about " + l)
			HComp.mechanicalTurk.sendQueryAndAwaitResult(FreetextQuery(
				s"""
				Hey there. Thank you for being interested in this task! In the following <a href="$l">URL</a> you'll find a Qualtrics-Survey showing you a text snipplet
				and asking you if two terms (highlighted in the text) do have a relationship of some sorts.
	  			Please fill in the survey and, once finished, enter the confirmation code below such that we can pay you. <br/>
	  Please note that you will only be able to answer the survey once (i.e. don't accept multiple assignments of this HIT) <br />
	<a href="$l">$l</a>
			 """.stripMargin, "", s"Are these two words in the text related? [${li._2}]"), HCompQueryProperties(50), 20 seconds)
		}
		catch {
			case e: Exception => e.printStackTrace()
		}
	})

	println("done")
}
