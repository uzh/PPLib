package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import java.io.{File, FileReader, IOException}
import java.net.{URL, URLEncoder}
import java.util.Properties
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Base64
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}

import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import scala.xml.{Node, NodeSeq, XML}

/**
 * taken from https://bitbucket.org/bethard/mturk/src/76b174f56c37a72e9d2188b05ef66c409dda7cc7/src/main/scala/info/bethard/mturk/mturk.scala?at=default
 * and adapted by PDB
 *
 */

private[mturk] case class CreatedHIT(xml: Node) {
	val HITId = MTurkUtil.oneText(xml \ "HITId")
	val HITTypeId = MTurkUtil.oneText(xml \ "HITTypeId")
}

private[mturk] case class HIT(xml: Node) {
	val HITId: String = MTurkUtil.oneText(xml \ "HITId")
	val HITTypeId = MTurkUtil.oneText(xml \ "HITTypeId")
	val CreationTime = MTurkUtil.oneDateTime(xml \ "CreationTime")
	val Title = MTurkUtil.oneText(xml \ "Title")
	val Description = MTurkUtil.oneText(xml \ "Description")
	val Keywords = MTurkUtil.oneText(xml \ "Keywords").split(",")
	val MaxAssignments = MTurkUtil.oneText(xml \ "MaxAssignments").toInt
	val Reward = Price(
		MTurkUtil.oneText(xml \ "Reward" \ "Amount"),
		MTurkUtil.oneText(xml \ "Reward" \ "CurrencyCode"))
	val AutoApprovalDelayInSeconds = MTurkUtil.oneText(
		xml \ "AutoApprovalDelayInSeconds").toInt
	val Expiration = MTurkUtil.oneDateTime(xml \ "Expiration")
	val RequesterAnnotation = MTurkUtil.oneTextOption(xml \ "RequesterAnnotation")
}

private[mturk] object AssignmentStatus {
	val Submitted = "Submitted"
	val Approved = "Approved"
	val Rejected = "Rejected"
}

private[mturk] case class Assignment(hit: String, xml: Node) {
	val AssignmentId = MTurkUtil.oneText(xml \ "AssignmentId")
	val WorkerId = MTurkUtil.oneText(xml \ "WorkerId")
	val HITId = hit
	val assignmentStatus = MTurkUtil.oneText(xml \ "AssignmentStatus")
	val AnswerXML = XML.loadString(MTurkUtil.oneText(xml \ "Answer"))
	val AcceptTime: Option[DateTime] = MTurkUtil.oneDateTimeOption(xml \ "AcceptTime")
	val SubmitTime: Option[DateTime] = MTurkUtil.oneDateTimeOption(xml \ "SubmitTime")
	val ApprovalTime = MTurkUtil.oneDateTimeOption(xml \ "ApprovalTime")
	val RejectionTime = MTurkUtil.oneDateTimeOption(xml \ "RejectionTime")

	def freeTextQuestionAnswerMap(): Map[String, String] = {
		var map = Map[String, String]()
		for (answerNode <- AnswerXML \ "Answer") yield {
			val question = MTurkUtil.oneText(answerNode \ "QuestionIdentifier")
			val answer = MTurkUtil.oneText(answerNode \ "FreeText")
			map(question) = answer
		}
		map
	}

	val isSubmitted = this.assignmentStatus == AssignmentStatus.Submitted
	val isApproved = this.assignmentStatus == AssignmentStatus.Approved
	val isRejected = this.assignmentStatus == AssignmentStatus.Rejected
}

private[mturk] sealed class Question(val xml: Node) {
	def trimmedXML = scala.xml.Utility.trim(xml)
}

private[mturk] object Question {

	private[mturk] case class ExternalQuestion(ExternalURL: String, FrameHeight: Int)
		extends Question(
			<ExternalQuestion xmlns="http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd">
				<ExternalURL>
					{ExternalURL}
				</ExternalURL>
				<FrameHeight>
					{FrameHeight}
				</FrameHeight>
			</ExternalQuestion>)

}

private[mturk] case class Price(val Amount: String, val CurrencyCode: String = "USD")


/**
 * {@link http://docs.amazonwebservices.com/AWSMturkAPI/2008-08-02/index.html?ApiReference_QualificationRequirementDataStructureArticle.html}
 */
private[mturk] case class QualificationRequirement(
									   QualificationTypeId: String,
									   Comparator: String,
									   RequiredToPreview: Boolean = false,
									   extraParameters: Seq[(String, String)] = Seq.empty) {

	/**
	 * Creates a new QualificationRequirement where RequiredToPreview is set to true.
	 */
	def andIsRequiredToPreview = this.copy(RequiredToPreview = true)

	/**
	 * Create the URL parameter name=value strings for the "index"-th requirement.
	 */
	private[mturk] def toParameterStrings(index: Int): Seq[String] = {
		val parameters = Seq(
			"QualificationTypeId" -> this.QualificationTypeId,
			"Comparator" -> this.Comparator,
			"RequiredToPreview" -> this.RequiredToPreview.toString)
		val prefix = "QualificationRequirement.%d".format(index)
		for ((name, value) <- parameters ++ this.extraParameters)
		yield "%s.%s=%s".format(prefix, name, URLEncoder.encode(value, "UTF8"))
	}
}

/**
 * {@link http://docs.amazonwebservices.com/AWSMturkAPI/2008-08-02/index.html?ApiReference_QualificationRequirementDataStructureArticle.html}
 */
private[mturk] object QualificationRequirement {

	/**
	 * Names of different Comparators used to specify QualificationRequirements.
	 */
	private[mturk] object Comparator {
		val LessThan = "LessThan"
		val LessThanOrEqualTo = "LessThanOrEqualTo"
		val GreaterThan = "GreaterThan"
		val GreaterThanOrEqualTo = "GreaterThanOrEqualTo"
		val EqualTo = "EqualTo"
		val NotEqualTo = "NotEqualTo"
		val Exists = "Exists"
	}

	/**
	 * The percentage of assignments the Worker has submitted, over all
	 * assignments the Worker has accepted. The value is an integer between
	 * 0 and 100.
	 */
	private[mturk] object Worker_PercentAssignmentsSubmitted
		extends IntegerValueFactory("00000000000000000000")

	/**
	 * The percentage of assignments the Worker has abandoned (allowed the
	 * deadline to elapse), over all assignments the Worker has accepted. The
	 * value is an integer between 0 and 100.
	 */
	private[mturk] object Worker_PercentAssignmentsAbandoned
		extends IntegerValueFactory("00000000000000000070")

	/**
	 * The percentage of assignments the Worker has returned, over all
	 * assignments the Worker has accepted. The value is an integer between
	 * 0 and 100.
	 */
	private[mturk] object Worker_PercentAssignmentsReturned
		extends IntegerValueFactory("000000000000000000E0")

	/**
	 * The percentage of assignments the Worker has submitted that were
	 * subsequently approved by the Requester, over all assignments the Worker
	 * has submitted. The value is an integer between 0 and 100.
	 */
	private[mturk] object Worker_PercentAssignmentsApproved
		extends IntegerValueFactory("000000000000000000L0")

	/**
	 * The percentage of assignments the Worker has submitted that were
	 * subsequently rejected by the Requester, over all assignments the Worker
	 * has submitted. The value is an integer between 0 and 100.
	 */
	private[mturk] object Worker_PercentAssignmentsRejected
		extends IntegerValueFactory("000000000000000000S0")

	/**
	 * Specifies the total number of HITs submitted by a Worker that have been
	 * approved. The value is an integer greater than or equal to 0.
	 */
	private[mturk] object Worker_NumberHITsApproved
		extends IntegerValueFactory("00000000000000000040")

	/**
	 * Requires workers to acknowledge that they are over 18 and that they agree
	 * to work on potentially offensive content. The value type is boolean,
	 * 1 (required), 0 (not required, the default)
	 */
	private[mturk] object Worker_Adult
		extends IntegerValueFactory("00000000000000000060")

	/**
	 * The location of the Worker, as specified in the Worker's mailing address.
	 */
	private[mturk] object Worker_Locale
		extends Factory[String]("00000000000000000071", "LocaleValue.Country")


	class Custom_QualificationType(val id: String) extends IntegerValueFactory(id)
	/**
	 * Base class for objects that create QualificationRequirements supporting
	 * the Exists, EqualTo and NotEqualTo comparators.
	 *
	 * Provides "exists", "===" and "!==" syntax for invoking comparators.
	 */
	private[mturk] abstract class Factory[VALUE](val QualificationTypeId: String, val valueName: String) {
		def exists = QualificationRequirement(this.QualificationTypeId, Comparator.Exists)

		def ===(value: VALUE) = this.comparing(Comparator.EqualTo, value)

		def !==(value: VALUE) = this.comparing(Comparator.NotEqualTo, value)

		def comparing(comparator: String, value: VALUE) = QualificationRequirement(
			QualificationTypeId = this.QualificationTypeId,
			Comparator = comparator,
			extraParameters = Seq(this.valueName -> value.toString))
	}

	/**
	 * Base class for objects that create QualificationRequirements that take an
	 * IntegerValue (and thus support all comparators).
	 *
	 * Provides "exists", "===", "!==", "<", "<=", ">" and ">=" syntax for
	 * invoking comparators.
	 */
	private[mturk] abstract class IntegerValueFactory(_QualificationTypeId: String)
		extends Factory[Int](_QualificationTypeId, "IntegerValue") {
		def <(value: Int) = this.comparing(Comparator.LessThan, value)

		def <=(value: Int) = this.comparing(Comparator.LessThanOrEqualTo, value)

		def >(value: Int) = this.comparing(Comparator.GreaterThan, value)

		def >=(value: Int) = this.comparing(Comparator.GreaterThanOrEqualTo, value)
	}

}

private[mturk] class Server(val url: String)

private[mturk] case object MTurkServer extends Server("https://mechanicalturk.amazonaws.com/?")

private[mturk] case object MTurkSandboxServer extends Server("http://mechanicalturk.sandbox.amazonaws.com/?")

private[mturk] object MTurkService {

	def apply(accessKey: String, secretKey: String, server: Server): MTurkService = {
		new MTurkService(accessKey, secretKey, server)
	}

	def apply(server: Server): MTurkService = {
		val userHome = System.getProperty("user.home")
		val possibleFiles = Seq(
			new File(".", "mturk.properties"),
			new File(".", ".mturk.properties"),
			new File(userHome, "mturk.properties"),
			new File(userHome, ".mturk.properties"))
		possibleFiles.filter(_.exists).headOption match {
			case None => {
				val message = "unable to find one of [%s]".format(possibleFiles.mkString(", "))
				throw new IllegalStateException(message)
			}
			case Some(file) => {
				val props = new Properties()
				val reader = new FileReader(file)
				try {
					props.load(reader)
				} finally {
					reader.close()
				}
				for (name <- Seq("access_key", "secret_key")) {
					if (!props.containsKey(name)) {
						val message = "%s does not have the \"%s\" property set".format(file, name)
						throw new IllegalArgumentException(message)
					}
				}
				new MTurkService(props("access_key"), props("secret_key"), server)
			}
		}
	}
}

private[mturk] class MTurkService(
					  accessKey: String,
					  secretKey: String,
					  val server: Server) {

	def RegisterHITType(
						   Title: String,
						   Description: String,
						   Reward: Price,
						   AssignmentDurationInSeconds: Int,
						   Keywords: Seq[String],
						   AutoApprovalDelayInSeconds: Int,
						   QualificationRequirements: Seq[QualificationRequirement]): String = {
		val simpleParams = Seq(
			"Title=" + URLEncoder.encode(Title, "UTF8"),
			"Description=" + URLEncoder.encode(Description, "UTF8"),
			"Reward.1.Amount=" + Reward.Amount,
			"Reward.1.CurrencyCode=" + Reward.CurrencyCode,
			"AssignmentDurationInSeconds=" + AssignmentDurationInSeconds,
			"Keywords=" + URLEncoder.encode(Keywords.mkString(","), "UTF8"),
			"AutoApprovalDelayInSeconds=" + AutoApprovalDelayInSeconds)
		val indexedReqs = QualificationRequirements.zipWithIndex
		val qualParams =
			for ((req, i) <- indexedReqs; param <- req.toParameterStrings(i + 1))
			yield param
		val params = simpleParams ++ qualParams
		val result = this.makeRequest("RegisterHITType", params: _*)
		MTurkUtil.oneText(result \ "RegisterHITTypeResult" \ "HITTypeId")
	}

	def CreateQualificationType(name: String, description: String = "Hits of this type can only be done once"): String = {
		val basicParams = Seq("Name=" + URLEncoder.encode(name, "UTF8"), "Description=" + URLEncoder.encode(description, "UTF8"), "QualificationTypeStatus=Active", "AutoGranted=true")
		val result = makeRequest("CreateQualificationType", basicParams: _*)
		(result \\ "QualificationTypeId").text
	}

	def UpdateQualificationScore(qualificationTypeID: String, workerId: String, newScore: Int) {
		val basicParams = Seq(s"QualificationTypeId=$qualificationTypeID", s"SubjectId=$workerId", s"IntegerValue=$newScore")
		makeRequest("UpdateQualificationScore", basicParams: _*)
	}

	def CreateHIT(
					 HITTypeId: String,
					 Question: Question,
					 LifetimeInSeconds: Int,
					 MaxAssignments: Int,
					 RequesterAnnotation: Option[String] = None): CreatedHIT = {
		val basicParams = Seq(
			"Question=" + URLEncoder.encode(Question.trimmedXML.toString, "UTF8"),
			"HITTypeId=" + URLEncoder.encode(HITTypeId, "UTF8"),
			"LifetimeInSeconds=" + LifetimeInSeconds,
			"MaxAssignments=" + MaxAssignments)
		val optionParam =
			for (value <- RequesterAnnotation)
			yield "RequesterAnnotation=" + URLEncoder.encode(value, "UTF8")
		val params = basicParams ++ optionParam.toSeq
		val result = this.makeRequest("CreateHIT", params: _*)
		CreatedHIT(MTurkUtil.oneNode(result \ "HIT"))
	}

	def CreateExternalQuestionHITs(
									  HITTypeId: String,
									  LifetimeInSeconds: Int,
									  MaxAssignments: Int,
									  FrameHeight: Int,
									  urls: Seq[String]): Iterator[CreatedHIT] = {
		val postedURLs = Set() ++ (
			for (hit <- this.SearchHITs; url <- hit.RequesterAnnotation)
			yield url)
		for (url <- urls.iterator.filter(url => !postedURLs.contains(url)))
		yield this.CreateHIT(
			Question = Question.ExternalQuestion(url, FrameHeight),
			HITTypeId = HITTypeId,
			LifetimeInSeconds = LifetimeInSeconds,
			MaxAssignments = MaxAssignments,
			RequesterAnnotation = Some(url))
	}

	def ChangeHITTypeOfHIT(hit: HIT, HITTypeId: String): Unit = {
		this.makeRequest("ChangeHITTypeOfHIT",
			"HITId=" + hit.HITId,
			"HITTypeId=" + HITTypeId)
	}

	def DisableHIT(hit: String): Unit = {
		this.makeRequest("DisableHIT", "HITId=" + hit)
	}

	def SearchHITs(): Iterable[HIT] = {
		val xmls = this.makePaginatedRequest("SearchHITs")
		for (xml <- xmls; node <- xml \\ "HIT") yield new HIT(node)
	}

	def SearchHITsByRequesterAnnotationPrefix(prefix: String) = {
		for {
			hit <- this.SearchHITs;
			ann <- hit.RequesterAnnotation;
			if ann.startsWith(prefix)
		} yield {
			hit
		}
	}

	def GetAssignmentsForHIT(hit: String): Iterable[Assignment] = {
		val xmls = this.makePaginatedRequest("GetAssignmentsForHIT", "HITId=" + hit)
		for (xml <- xmls; node <- xml \\ "Assignment") yield new Assignment(hit, node)
	}

	def GetAssignmentsSubmittedForHIT(hit: String): Iterable[Assignment] = {
		this.GetAssignmentsForHIT(hit, AssignmentStatus.Submitted)
	}

	def GetAssignmentsApprovedForHIT(hit: String): Iterable[Assignment] = {
		this.GetAssignmentsForHIT(hit, AssignmentStatus.Approved)
	}

	def GetAssignmentsRejectedForHIT(hit: String): Iterable[Assignment] = {
		this.GetAssignmentsForHIT(hit, AssignmentStatus.Rejected)
	}

	private def GetAssignmentsForHIT(hit: String, AssignmentStatus: String): Iterable[Assignment] = {
		val xmls = this.makePaginatedRequest("GetAssignmentsForHIT",
			"HITId=" + hit,
			"AssignmentStatus=" + AssignmentStatus)
		for (xml <- xmls; node <- xml \\ "Assignment") yield new Assignment(hit, node)
	}

	def ApproveAssignment(assignment: Assignment): Unit = {
		this.ApproveAssignment(assignment.AssignmentId)
	}

	def ApproveAssignment(assignment: Assignment, feedback: String): Unit = {
		this.makeRequest("ApproveAssignment",
			"AssignmentId=" + assignment.AssignmentId,
			"RequesterFeedback=" + URLEncoder.encode(feedback, "UTF-8"))
	}

	def ApproveAssignment(AssignmentId: String): Unit = {
		this.makeRequest("ApproveAssignment", "AssignmentId=" + AssignmentId)
	}

	def ApproveAssignmentURL(AssignmentId: String): String = {
		this.makeRequestURL("ApproveAssignment", "AssignmentId=" + AssignmentId)
	}

	def RejectAssignment(assignment: Assignment, feedback: String): Unit = {
		this.RejectAssignment(assignment.AssignmentId, feedback)
	}

	def RejectAssignment(AssignmentId: String, feedback: String): Unit = {
		this.makeRequest(
			"RejectAssignment",
			"AssignmentId=" + AssignmentId,
			"RequesterFeedback=" + URLEncoder.encode(feedback, "UTF-8"))
	}

	def RejectAndRepostAssignment(assignment: Assignment, feedback: String): Unit = {
		this.RejectAndRepostAssignment(assignment.AssignmentId, assignment.hit, feedback)
	}

	def RejectAndRepostAssignment(AssignmentId: String, HITId: String, feedback: String): Unit = {
		this.RejectAssignment(AssignmentId, feedback)
		this.ExtendHITMaxAssignments(HITId, 1)
	}

	def BlockWorker(workerId: String, reason: String): Unit = {
		this.makeRequest(
			"BlockWorker",
			"WorkerId=" + workerId,
			"Reason=" + URLEncoder.encode(reason, "UTF-8"))
	}

	def ExtendHITMaxAssignments(hit: HIT, MaxAssignmentsIncrement: Int): Unit = {
		this.ExtendHITMaxAssignments(hit.HITId, MaxAssignmentsIncrement)
	}

	def ExtendHITMaxAssignments(HITId: String, MaxAssignmentsIncrement: Int): Unit = {
		this.makeRequest(
			"ExtendHIT",
			"HITId=" + HITId,
			"MaxAssignmentsIncrement=" + MaxAssignmentsIncrement)
	}

	def ExtendHITExpiration(hit: HIT, ExpirationIncrementInSeconds: Int): Unit = {
		this.ExtendHITExpiration(hit.HITId, ExpirationIncrementInSeconds)
	}

	def ExtendHITExpiration(HITId: String, ExpirationIncrementInSeconds: Int): Unit = {
		this.makeRequest(
			"ExtendHIT",
			"HITId=" + HITId,
			"ExpirationIncrementInSeconds=" + ExpirationIncrementInSeconds)
	}

	def makeRequestURL(operation: String, extraParameters: String*): String = {
		val service = "AWSMechanicalTurkRequester"
		val timeStamp = MTurkUtil.dateTimeFormatter.print(new DateTime)
		val signatureData = service + operation + timeStamp
		val signature = this.calculateRFC2104HMAC(signatureData, secretKey)
		val parameters = List(
			"Service=" + service,
			"Version=2008-08-02",
			"AWSAccessKeyId=" + accessKey,
			"Signature=" + URLEncoder.encode(signature, "UTF-8"),
			"Timestamp=" + timeStamp,
			"Operation=" + operation) ++ extraParameters
		server.url + parameters.mkString("&")
	}

	def makeRequest(operation: String, extraParameters: String*): Node = {
		val url = this.makeRequestURL(operation, extraParameters: _*)
		val tries = Iterator.continually {
			try {
				Some(XML.load(new URL(url)))
			} catch {
				case e: IOException => {
					Thread.sleep(500)
					None
				}
			}
		}
		val successes = tries.take(5).map(_.iterator).flatten
		if (!successes.hasNext) {
			throw new Exception("unable to connect to " + url)
		}
		val xml = successes.next
		if ((xml \\ "Error").size > 0) {
			throw new Exception(xml.toString.replaceAll("<", "\n<"))
		}
		xml
	}

	def makePaginatedRequest(operation: String, extraParameters: String*): Iterable[Node] = {
		val getPage = (i: Int) => {
			val params = Array("PageSize=100", "PageNumber=" + i) ++ extraParameters
			this.makeRequest(operation, params: _*)
		}
		val containsHITs = (xml: Node) => (xml \\ "NumResults").text.toInt > 0
		Stream.from(1).map(getPage).takeWhile(containsHITs)
	}

	private def calculateRFC2104HMAC(data: String, key: String): String = {
		val algorithm = "HmacSHA1"
		val signingKey = new SecretKeySpec(key.getBytes(), algorithm)
		val mac = Mac.getInstance(algorithm)
		mac.init(signingKey)
		val rawHMAC = mac.doFinal(data.getBytes())
		new String(new Base64().encode(rawHMAC)).trim()
	}
}

private object MTurkUtil {
	val dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis.withZone(DateTimeZone.UTC)

	def oneNode(nodes: NodeSeq): Node = {
		val Seq(node) = nodes
		node
	}

	def oneText(nodes: NodeSeq): String = {
		this.oneNode(nodes).text
	}

	def oneTextOption(nodes: NodeSeq): Option[String] = {
		if (nodes.size == 0) {
			None
		} else {
			Some(this.oneText(nodes))
		}
	}

	def oneDateTime(nodes: NodeSeq): DateTime = {
		this.dateTimeFormatter.parseDateTime(this.oneText(nodes))
	}

	def oneDateTimeOption(nodes: NodeSeq): Option[DateTime] = {
		for (text <- this.oneTextOption(nodes))
		yield this.dateTimeFormatter.parseDateTime(text)
	}
}