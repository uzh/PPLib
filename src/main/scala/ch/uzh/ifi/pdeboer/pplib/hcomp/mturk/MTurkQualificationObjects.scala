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
			case e: QualificationType.PercentAssignmentsSubmitted => Some(QualificationRequirement.Worker_PercentAssignmentsSubmitted)
			case e: QualificationType.PercentAssignmentsAbandoned => Some(QualificationRequirement.Worker_PercentAssignmentsAbandoned)
			case e: QualificationType.PercentAssignmentsReturned => Some(QualificationRequirement.Worker_PercentAssignmentsReturned)
			case e: QualificationType.PercentAssignmentsApproved => Some(QualificationRequirement.Worker_PercentAssignmentsApproved)
			case e: QualificationType.PercentAssignmentsRejected => Some(QualificationRequirement.Worker_PercentAssignmentsRejected)
			case e: QualificationType.NumberHITsApproved => Some(QualificationRequirement.Worker_NumberHITsApproved)
			case e: QualificationType.Adult => Some(QualificationRequirement.Worker_Adult)
			case e => None
		}

		val isStringBased: Option[Factory[String]] = workerQualification.id match {
			case e: QualificationType.Locale => Some(QualificationRequirement.Worker_Locale)
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
