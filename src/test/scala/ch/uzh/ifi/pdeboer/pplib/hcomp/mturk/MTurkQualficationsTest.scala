package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp.QualificationType.PercentAssignmentsSubmitted
import ch.uzh.ifi.pdeboer.pplib.hcomp.QueryWorkerQualification
import ch.uzh.ifi.pdeboer.pplib.hcomp.mturk.QualificationRequirement.Worker_PercentAssignmentsSubmitted
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 24/12/14.
 */
class MTurkQualficationsTest {
	@Test
	def testConversion: Unit = {
		val b: QueryWorkerQualification = new PercentAssignmentsSubmitted() > "1"
		val actual = MTurkQualificationObjectConversion.toMTurkQualificationRequirement(b)
		val expected: QualificationRequirement = Worker_PercentAssignmentsSubmitted > 1
		Assert.assertEquals(expected, actual)
	}
}
