package ch.uzh.ifi.pdeboer.pplib.hcomp.mturk

import ch.uzh.ifi.pdeboer.pplib.hcomp.{MultipleChoiceQuery, FreetextQuery}
import org.junit.{Test, Assert}

/**
 * Created by pdeboer on 20/11/14.
 */
class MTQueryTest {
	@Test
	def testFreetextQueryXML(): Unit = {
		val q = FreetextQuery("question", defaultAnswer = "default", title = "title")
		val mtQ = MTQuery.convert(q).asInstanceOf[MTFreeTextQuery]
		Assert.assertEquals(
			removeWhitespaces(<FreeTextAnswer>
				<DefaultText>default</DefaultText>
			</FreeTextAnswer>.toString)
			, removeWhitespaces(mtQ.elementXML + ""))
	}

	@Test
	def testRemoveWhitespaces: Unit = {
		Assert.assertEquals("addfs", removeWhitespaces("a d	d\nf s"))
	}

	def removeWhitespaces(str: String) = str.replaceAll("\\s*", "")

	@Test
	def testFreetextQueryXMLNoDefault(): Unit = {
		val q = FreetextQuery("question")
		val mtQ = MTQuery.convert(q).asInstanceOf[MTFreeTextQuery]
		Assert.assertEquals(
			removeWhitespaces(<FreeTextAnswer></FreeTextAnswer>.toString())
			, removeWhitespaces(mtQ.elementXML.toString()))
	}

	@Test
	def testMultipleChoice: Unit = {
		val q = MultipleChoiceQuery("qestion", List("a", "b", "c"), 2, 1)
		val mtQ = MTQuery.convert(q).asInstanceOf[MTMultipleChoiceQuery]
		Assert.assertEquals(
			removeWhitespaces(<SelectionAnswer>
				<MinSelectionCount>1</MinSelectionCount>
				<MaxSelectionCount>2</MaxSelectionCount>
			</SelectionAnswer>.toString()), removeWhitespaces(mtQ.elementXML.toString()))
	}
}
