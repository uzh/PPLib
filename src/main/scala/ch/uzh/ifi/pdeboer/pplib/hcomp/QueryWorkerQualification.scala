package ch.uzh.ifi.pdeboer.pplib.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.QueryComparator._

/**
 * Created by pdeboer on 24/12/14.
 */
@SerialVersionUID(1L)
case class QueryWorkerQualification(id: QualificationType[_], comparator: QueryComparator, value: String) extends Serializable {
	override def toString: String = id + " " + comparator + " " + value
}

@SerialVersionUID(1L)
abstract class QualificationType[T] extends Serializable {
	def value: String

	def <(value: T) = QueryWorkerQualification(this, QueryComparator.LessThan(), value.toString)

	def >(value: T) = QueryWorkerQualification(this, QueryComparator.GreaterThan(), value.toString)

	def <=(value: T) = QueryWorkerQualification(this, LessThanOrEqualTo(), value.toString)

	def >=(value: T) = QueryWorkerQualification(this, GreaterThanOrEqualTo(), value.toString)

	def ===(value: T) = QueryWorkerQualification(this, EqualTo(), value.toString)

	def !===(value: T) = QueryWorkerQualification(this, NotEqualTo(), value.toString)

	def ?(value: T) = QueryWorkerQualification(this, Exists(), value.toString)
}

object QualificationType {
	class ClassNameForwarder[T] extends QualificationType[T] {
		def value = getClass.getSimpleName.substring(2)

		override def toString: String = value
	}

	class QTPercentAssignmentsSubmitted extends ClassNameForwarder[Int]

	class QTPercentAssignmentsAbandoned extends ClassNameForwarder[Int]

	class QTPercentAssignmentsReturned extends ClassNameForwarder[Int]

	class QTPercentAssignmentsApproved extends ClassNameForwarder[Int]

	class QTPercentAssignmentsRejected extends ClassNameForwarder[Int]

	class QTNumberHITsApproved extends ClassNameForwarder[Int]

	class QTAdult extends ClassNameForwarder[Int]

	class QTLocale extends ClassNameForwarder[String]

}

@SerialVersionUID(1L)
sealed trait QueryComparator extends Serializable {
	def value: String

	override def toString: String = value
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