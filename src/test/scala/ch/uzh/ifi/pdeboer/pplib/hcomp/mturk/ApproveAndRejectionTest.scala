package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextAnswer, FreetextQuery}
import org.junit.{Assert, Test}
import org.mockito.Mockito._

/**
 * Created by pdeboer on 07/07/15.
 */
class ApproveAndRejectionTest {

	private class EverythingAlrightException extends RuntimeException

	@Test
	def testApproveMethodCalledOnService: Unit = {
		val assignment = mock(classOf[Assignment])
		val serviceMock = mock(classOf[MTurkService])

		val hcompAnswer = FreetextAnswer(FreetextQuery("asdf"), "asdf", Nil)

		val rejectable = new RejectableTurkAnswer(assignment, hcompAnswer, serviceMock)
		val testAdapter = new MechanicalTurkPortalAdapter("asdf", "asdf", approveAll = false)
		testAdapter.addUnapprovedAnswer(rejectable)
		Assert.assertEquals(1, testAdapter.getUnapprovedAnswers.size)

		testAdapter.approveAndBonusAnswer(hcompAnswer, "msg")

		verify(serviceMock).ApproveAssignment(assignment, "msg")

		Assert.assertEquals(0, testAdapter.getUnapprovedAnswers.size)
	}

	@Test
	def testRejectMethodCalledOnService: Unit = {
		val assignment = mock(classOf[Assignment])
		val serviceMock = mock(classOf[MTurkService])

		val hcompAnswer = FreetextAnswer(FreetextQuery("asdf"), "asdf", Nil)

		val rejectable = new RejectableTurkAnswer(assignment, hcompAnswer, serviceMock)
		val testAdapter = new MechanicalTurkPortalAdapter("asdf", "asdf", approveAll = false)
		testAdapter.addUnapprovedAnswer(rejectable)
		Assert.assertEquals(1, testAdapter.getUnapprovedAnswers.size)

		testAdapter.rejectAnswer(hcompAnswer, "msg")

		verify(serviceMock).RejectAssignment(assignment, "msg")

		Assert.assertEquals(0, testAdapter.getUnapprovedAnswers.size)
	}
}
