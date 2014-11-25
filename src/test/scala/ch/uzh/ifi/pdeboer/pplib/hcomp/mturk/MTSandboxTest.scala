package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp.{FreetextAnswer, HCompQueryProperties, FreetextQuery, HComp}
import org.junit.Test

/**
 * Created by pdeboer on 21/11/14.
 */
class MTSandboxTest {
	@Test
	def testSendTextBox: Unit = {
		val r = HComp.mechanicalTurk.sendQueryAndAwaitResult(FreetextQuery("what's your name? <b>nothing much</b>"), HCompQueryProperties(5d))
		val answer = r.get.as[FreetextAnswer]
		println(answer.answer)
	}
}
