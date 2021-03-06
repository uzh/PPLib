package ch.uzh.ifi.pdeboer.pplib.hcomp

import ch.uzh.ifi.pdeboer.pplib.util.U
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 17/12/14.
 */
class HCompInstructionsWithTupleTest {
	@Test
	def testHTMLElementRenderingDisabledInStringified: Unit = {
		var res = (StringQuestionRenderer("<p>test</p>").getInstructions(""))
		Assert.assertEquals("<p>&lt;p&gt;test&lt;/p&gt;</p>", U.removeWhitespaces(res))
	}

	@Test
	def testHTMLRenderingAllowedInNormal: Unit = {
		val res = new BasicQuestionRenderer(<asdf>question?</asdf>)
		Assert.assertEquals("<p><asdf>question?</asdf></p>", U.removeWhitespaces(res.getInstructions("")))
	}
}
