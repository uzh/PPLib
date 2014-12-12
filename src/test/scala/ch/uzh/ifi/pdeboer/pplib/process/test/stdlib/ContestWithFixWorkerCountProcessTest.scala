package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.ProcessStubWithHCompPortalAccess
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithFixWorkerCountProcess
import org.junit.Assert

/**
 * Created by pdeboer on 03/11/14.
 *
 */
class ContestWithFixWorkerCountProcessTest {
	//@Test
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
					portal.synchronized {
						val target = votes.filter(_._2 > 0).toList.head._1
						votes += target -> (votes(target) - 1)
						Some(MultipleChoiceAnswer(mq, data.map(d => {
							(d, d.equals(target))
						}).toMap))
					}
				}
				case _ => None
			}

		}
		portal.filters ::= f

		val subject = new ContestWithFixWorkerCountProcess(
			Map(ContestWithFixWorkerCountProcess.WORKER_COUNT.key -> 3,
				ProcessStubWithHCompPortalAccess.PORTAL_PARAMETER.key -> portal,
				ProcessStubWithHCompPortalAccess.PARALLEL_EXECUTION_PARAMETER.key -> false,
				ContestWithFixWorkerCountProcess.INSTRUCTIONS.key -> HCompInstructionsWithTuple("")))

		val res = subject.run(data)
		Assert.assertEquals(data(0), res)
		Assert.assertEquals(0, votes.values.sum)
	}

	//@Test
	def testWorkerCountAgreement5: Unit = {
		val portal = new MockHCompPortal

		val data: List[String] = 1 to 5 map (_.toString) toList

		var votes = collection.mutable.HashMap[String, Int](
			data(0) -> 4, //2 votes
			data(1) -> 2, //1
			data(2) -> 2,
			data(3) -> 2
		)

		val f = (q: HCompQuery) => {
			q match {
				case mq: MultipleChoiceQuery => {
					HComp.synchronized {
						val target = votes.filter(_._2 > 0).toList.head._1
						votes += target -> (votes(target) - 1)
						Some(MultipleChoiceAnswer(mq, data.map(d => {
							(d, d.equals(target))
						}).toMap))
					}
				}
				case _ => None
			}

		}
		portal.filters ::= f

		val subject = new ContestWithFixWorkerCountProcess(
			Map(ContestWithFixWorkerCountProcess.WORKER_COUNT.key -> 5,
				ProcessStubWithHCompPortalAccess.PORTAL_PARAMETER.key -> portal,
				ProcessStubWithHCompPortalAccess.PARALLEL_EXECUTION_PARAMETER.key -> false,
				ContestWithFixWorkerCountProcess.INSTRUCTIONS.key -> HCompInstructionsWithTuple("")))

		val res = subject.run(data)
		Assert.assertEquals(data(0), res)
		Assert.assertEquals(0, votes.values.sum)
	}
}
