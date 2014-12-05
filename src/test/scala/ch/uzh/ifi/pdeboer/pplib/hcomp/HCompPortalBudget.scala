package ch.uzh.ifi.pdeboer.pplib.hcomp

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 02/12/14.
 */
class HCompPortalBudget {
	@Test
	def testBudgetingNoBudgetSet: Unit = {
		val p = new MyTestPortal
		//default is no budget

		Assert.assertEquals(Some(a), p.sendQueryAndAwaitResult(q))
	}

	@Test
	def testBudgetingWithBudget: Unit = {
		val p = new MyTestPortal
		p.setBudget(Some(10)) //10 cents in total
		Assert.assertEquals(Some(a), p.sendQueryAndAwaitResult(q, HCompQueryProperties(1)))
		Assert.assertEquals(9, p.budget.get)

		Assert.assertEquals(Some(a), p.sendQueryAndAwaitResult(q, HCompQueryProperties(paymentCents = 9)))
		Assert.assertEquals(0, p.budget.get)

		Assert.assertEquals(None, p.sendQueryAndAwaitResult(q, HCompQueryProperties(1)))
	}

	val q = new FreetextQuery("test")
	val a = new FreetextAnswer(q, "worked")

	private class MyTestPortal extends HCompPortalAdapter {
		//TODO we should hide this method somehow to the public
		override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = Some(a)

		override def getDefaultPortalKey: String = "testportalbudget"

		override def cancelQuery(query: HCompQuery): Unit = {}
	}

}
