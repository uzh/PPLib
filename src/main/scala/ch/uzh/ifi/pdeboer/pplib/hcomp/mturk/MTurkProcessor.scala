package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.{GrowingTimer, U}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 19/11/14.
 */
class MTurkManager(val service: MTurkService, val query: HCompQuery, val properties: HCompQueryProperties) extends LazyLogging {
	var hit = ""
	var cancelled: Boolean = false

	def waitForResponse() = {
		val timer = new GrowingTimer(1 second, 1.1, 30 seconds)
		U.retry(100000) {
			//at least 27h
			if (!cancelled) {
				timer.waitTime

				val answer = poll()
				if (answer == None) throw new IllegalStateException("let's wait some more")
				answer
			} else None
		}
	}

	def cancelHIT(): Unit = {
		cancelled = true
		service.DisableHIT(hit)
		//service.deleteHITs(Array(hit.getHITId), true, true, emptyCallback)
	}

	/**
	 * @return HIT ID
	 */
	def createHIT(): String = {
		val mtQuery = MTQuery.convert(query)

		val ONE_DAY: Int = 60 * 60 * 24
		val dollars: Double = properties.paymentCents.toDouble / 100d
		val hitTypeID = service.RegisterHITType(query.title, query.question, Price(dollars.toString), ONE_DAY, Seq.empty[String], ONE_DAY, Seq.empty[QualificationRequirement])
		hit = service.CreateHIT(hitTypeID, new Question(mtQuery.xml), ONE_DAY, 1).HITId
		hit
	}

	def poll(): Option[HCompAnswer] = {
		logger.debug("checking for answer..")
		val assignments = service.GetAssignmentsForHIT(hit)
		assignments.headOption match {
			case None => None
			case Some(a: Assignment) => handleAssignmentResult(a)
		}
	}

	def handleAssignmentResult(a: Assignment): Some[HCompAnswer] = {
		try {
			//We approve all assignments by default. Don't like rejections
			service.ApproveAssignment(a)
		}
		catch {
			case e: Exception => logger.error("could not approve assignment", e)
		}

		val xml = a.AnswerXML
		val answer: HCompAnswer = MTQuery.convert(query).interpret(xml)
		answer.acceptTime = a.AcceptTime
		answer.submitTime = a.SubmitTime
		Some(answer)
	}

	def hitXML(question: NodeSeq) =
		<QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
			{question}
		</QuestionForm>
}
