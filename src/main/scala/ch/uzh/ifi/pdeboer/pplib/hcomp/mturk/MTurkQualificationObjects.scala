package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp.mturk.QualificationRequirement.{Factory, IntegerValueFactory}
import ch.uzh.ifi.pdeboer.pplib.hcomp.{QualificationType, QueryWorkerQualification}
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

/**
 * Created by pdeboer on 24/12/14.
 */
object MTurkQualificationObjectConversion extends LazyLogger {
	implicit def toMTurkQualificationRequirement(workerQualification: QueryWorkerQualification): QualificationRequirement = {
		val isIntegerBased: Option[IntegerValueFactory] = workerQualification.id match {
			case e: QualificationType.QTPercentAssignmentsSubmitted => Some(QualificationRequirement.Worker_PercentAssignmentsSubmitted)
			case e: QualificationType.QTPercentAssignmentsAbandoned => Some(QualificationRequirement.Worker_PercentAssignmentsAbandoned)
			case e: QualificationType.QTPercentAssignmentsReturned => Some(QualificationRequirement.Worker_PercentAssignmentsReturned)
			case e: QualificationType.QTPercentAssignmentsApproved => Some(QualificationRequirement.Worker_PercentAssignmentsApproved)
			case e: QualificationType.QTPercentAssignmentsRejected => Some(QualificationRequirement.Worker_PercentAssignmentsRejected)
			case e: QualificationType.QTNumberHITsApproved => Some(QualificationRequirement.Worker_NumberHITsApproved)
			case e: QualificationType.QTAdult => Some(QualificationRequirement.Worker_Adult)
			case e => None
		}

		val isStringBased: Option[Factory[String]] = workerQualification.id match {
			case e: QualificationType.QTLocale => Some(QualificationRequirement.Worker_Locale)
			case e => None
		}

		if (isIntegerBased.isDefined) {
			isIntegerBased.get.comparing(workerQualification.comparator.value, workerQualification.value.toInt)
		} else if (isStringBased.isDefined) {
			isStringBased.get.comparing(workerQualification.comparator.value, workerQualification.value)
		} else {
			logger.error("Could not find MTurk equivalent for " + workerQualification)
			null
		}
	}
}
