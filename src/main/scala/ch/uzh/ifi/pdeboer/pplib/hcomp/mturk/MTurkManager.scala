package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.util.{GrowingTimer, LazyLogger}

import scala.concurrent.duration._
import scala.util.Random
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 19/11/14.
 */
class MTurkManager(val query: HCompQuery, val properties: HCompQueryProperties, val adapter: MechanicalTurkPortalAdapter) extends LazyLogger {
	var hit = ""
	var cancelled: Boolean = false

	private val service = adapter.service

	def waitForResponse() = {
		val timer = new GrowingTimer(1 second, 1.0001, 20 seconds)
		//very very ugly, but we dont have a break statement in scala..
		var answer: Option[HCompAnswer] = None
		try {
			(1 to 1000000).view.foreach(i => {
				answer = poll()
				if (cancelled || answer.isDefined) throw new Exception("I'm actually not an Exception")
				timer.waitTime
			})
			logger.info(s"got timeout waiting for an answer for $query")
		}
		catch {
			case e: Exception => {
				/*hopefully we land here*/
				logger.info("received response " + poll())
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
	def createHIT(numAssignments: Int = 1): String = {
		if (scala.xml.PCData(query.question).length > 1999)
			logger.error("your question was longer than 1999 characters, which is not allowed by MTurk. Truncated the question to " + query.question.take(1999))
		val mtQuery = MTQuery.convert(query)

		val ONE_DAY: Int = 60 * 60 * 24
		val TEN_MINUTES: Int = 10 * 60
		val dollars: Double = adapter.price(query, properties).toDouble / 100d

		val qualifications: List[QualificationRequirement] = properties.qualifications.map(q =>
			MTurkQualificationObjectConversion.toMTurkQualificationRequirement(q)).filterNot(_ == null)

		val rnd = Math.abs(Random.nextInt()) + ""
		val title: String = query.title.take(117 - rnd.length) + " [" + rnd + "]"
		val hitTypeID = service.RegisterHITType(title, query.questionPreview, Price(dollars.toString), TEN_MINUTES, Seq.empty[String], 5 * ONE_DAY, qualifications)
		hit = service.CreateHIT(hitTypeID, new Question(mtQuery.xml), ONE_DAY, numAssignments).HITId
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
		val xml = a.AnswerXML
		val convertedAnswer = MTQuery.convert(query).interpret(xml, a.WorkerId)
		convertedAnswer.map(answer => {
			answer.acceptTime = a.AcceptTime
			answer.submitTime = a.SubmitTime

			val ans: RejectableTurkAnswer = new RejectableTurkAnswer(a, answer, service)
			if (adapter.approveAll)
				ans.approve("")
			else
				adapter.addUnapprovedAnswer(ans)

			answer
		})
	}

	def hitXML(question: NodeSeq) =
		<QuestionForm xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd">
			{question}
		</QuestionForm>
}
