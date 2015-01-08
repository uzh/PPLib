package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.{GrowingTimer, LazyLogger}

import scala.concurrent.duration._
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 19/11/14.
 */
class MTurkManager(val service: MTurkService, val query: HCompQuery, val properties: HCompQueryProperties) extends LazyLogger {
	var hit = ""
	var cancelled: Boolean = false

	def waitForResponse() = {
		val timer = new GrowingTimer(1 second, 1.0001, 20 seconds)
		//very very ugly, but we dont have a break statement in scala..
		var answer: Option[HCompAnswer] = None
		try {
			(1 to 100000).view.foreach(i => {
				answer = poll()
				if (cancelled || answer.isDefined) throw new Exception("I'm actually not an Exception")
				timer.waitTime
			})
		}
		catch {
			case e: Exception => {
				/*hopefully we land here*/
			}
		}
		answer
	}

	def cancelHIT(): Unit = {
		cancelled = true
		try {
			service.DisableHIT(hit)
		}
		catch {
			case e: Throwable => logger.error(s"got exception when trying to cancel query '${query.question}'", e)
		}
		//service.deleteHITs(Array(hit.getHITId), true, true, emptyCallback)
	}

	/**
	 * @return HIT ID
	 */
	def createHIT(): String = {
		if (scala.xml.PCData(query.question).length > 1999)
			logger.error("your question was longer than 1999 characters, which is not allowed by MTurk. Truncated the question to " + query.question.take(1999))
		val mtQuery = MTQuery.convert(query)

		val ONE_DAY: Int = 60 * 60 * 24
		val TEN_MINUTES: Int = 10 * 60
		val dollars: Double = properties.paymentCents.toDouble / 100d

		val qualifications: List[QualificationRequirement] = properties.qualifications.map(q =>
			MTurkQualificationObjectConversion.toMTurkQualificationRequirement(q)).filterNot(_ == null)

		val hitTypeID = service.RegisterHITType(query.title.take(120), query.question, Price(dollars.toString), TEN_MINUTES, Seq.empty[String], ONE_DAY, qualifications)
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

	def handleAssignmentResult(a: Assignment): Option[HCompAnswer] = {
		try {
			//We approve all assignments by default. Don't like rejections
			service.ApproveAssignment(a)
		}
		catch {
			case e: Exception => logger.error("could not approve assignment", e)
		}

		val xml = a.AnswerXML
		val answer = MTQuery.convert(query).interpret(xml)
		answer.map(answer => {
			answer.acceptTime = a.AcceptTime
			answer.submitTime = a.SubmitTime
			answer
		})
	}

	def hitXML(question: NodeSeq) =
		<QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
			{question}
		</QuestionForm>
}
