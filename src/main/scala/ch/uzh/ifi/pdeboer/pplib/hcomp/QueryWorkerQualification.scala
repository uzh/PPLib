package ch.uzh.ifi.pdeboer.pplib.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.QueryComparator._

/**
 * Created by pdeboer on 24/12/14.
 */
@SerialVersionUID(1L)
case class QueryWorkerQualification(id: QualificationType, comparator: QueryComparator, value: String) extends Serializable

@SerialVersionUID(1L)
abstract class QualificationType extends Serializable {
	def value: String

	def <(value: String) = QueryWorkerQualification(this, QueryComparator.LessThan(), value)

	def >(value: String) = QueryWorkerQualification(this, QueryComparator.GreaterThan(), value)

	def <=(value: String) = QueryWorkerQualification(this, LessThanOrEqualTo(), value)

	def >=(value: String) = QueryWorkerQualification(this, GreaterThanOrEqualTo(), value)

	def ===(value: String) = QueryWorkerQualification(this, EqualTo(), value)

	def !===(value: String) = QueryWorkerQualification(this, NotEqualTo(), value)

	def ?(value: String) = QueryWorkerQualification(this, Exists(), value)
}

object QualificationType {

	class ClassNameForwarder extends QualificationType {
		def value = getClass.getSimpleName
	}

	class PercentAssignmentsSubmitted extends ClassNameForwarder

	class PercentAssignmentsAbandoned extends ClassNameForwarder

	class PercentAssignmentsReturned extends ClassNameForwarder

	class PercentAssignmentsApproved extends ClassNameForwarder

	class PercentAssignmentsRejected extends ClassNameForwarder

	class NumberHITsApproved extends ClassNameForwarder

	class Adult extends ClassNameForwarder

	class Locale extends ClassNameForwarder

}

@SerialVersionUID(1L)
sealed trait QueryComparator extends Serializable {
	def value: String
}

object QueryComparator {

	case class LessThan(value: String = "LessThan") extends QueryComparator

	case class LessThanOrEqualTo(value: String = "LessThanOrEqualTo") extends QueryComparator

	case class GreaterThan(value: String = "GreaterThan") extends QueryComparator

	case class GreaterThanOrEqualTo(value: String = "GreaterThanOrEqualTo") extends QueryComparator

	case class EqualTo(value: String = "EqualTo") extends QueryComparator

	case class NotEqualTo(value: String = "NotEqualTo") extends QueryComparator

	case class Exists(value: String = "Exists") extends QueryComparator

}