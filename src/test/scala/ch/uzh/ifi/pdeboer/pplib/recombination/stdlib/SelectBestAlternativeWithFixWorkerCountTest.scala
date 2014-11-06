package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination.{HCompPortalAccess, RecombinationStub}
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 03/11/14.
 */
class SelectBestAlternativeWithFixWorkerCountTest {
	@Test
	def testWorkerCountAgreement3: Unit = {
		val portal = new MockHCompPortal

		val data = 1 to 5 map (_.toString) toList

		var votes = collection.mutable.HashMap[String, Int](
			data(0) -> 4, //2 votes
			data(1) -> 2 //1
		)

		val f = (q: HCompQuery) => {
			q match {
				case mq: MultipleChoiceQuery => {
					val target = votes.filter(_._2 > 0).toList.head._1
					votes += target -> (votes(target) - 1)
					Some(MultipleChoiceAnswer(mq, data.map(d => {
						(d, d.equals(target))
					}).toMap))
				}
				case _ => None
			}

		}
		portal.filters ::= f

		val subject = new SelectBestAlternativeWithFixWorkerCount(
			Map(SelectBestAlternativeWithFixWorkerCount.WORKER_COUNT_PARAMETER.key -> 3,
				HCompPortalAccess.PORTAL_PARAMETER.key -> portal,
				SelectBestAlternativeWithFixWorkerCount.INSTRUCTIONS_PARAMETER.key -> HCompInstructionsWithData("")))

		val res = subject.run(data)
		Assert.assertEquals(data(0), res)
		Assert.assertEquals(0, votes.values.sum)
	}

	@Test
	def testWorkerCountAgreement5: Unit = {
		val portal = new MockHCompPortal

		val data = 1 to 5 map (_.toString) toList

		var votes = collection.mutable.HashMap[String, Int](
			data(0) -> 4, //2 votes
			data(1) -> 2, //1
			data(2) -> 2,
			data(3) -> 2
		)

		val f = (q: HCompQuery) => {
			q match {
				case mq: MultipleChoiceQuery => {
					val target = votes.filter(_._2 > 0).toList.head._1
					votes += target -> (votes(target) - 1)
					Some(MultipleChoiceAnswer(mq, data.map(d => {
						(d, d.equals(target))
					}).toMap))
				}
				case _ => None
			}

		}
		portal.filters ::= f

		val subject = new SelectBestAlternativeWithFixWorkerCount(
			Map(SelectBestAlternativeWithFixWorkerCount.WORKER_COUNT_PARAMETER.key -> 5,
				HCompPortalAccess.PORTAL_PARAMETER.key -> portal,
				SelectBestAlternativeWithFixWorkerCount.INSTRUCTIONS_PARAMETER.key -> HCompInstructionsWithData("")))

		val res = subject.run(data)
		Assert.assertEquals(data(0), res)
		Assert.assertEquals(0, votes.values.sum)
	}
}
