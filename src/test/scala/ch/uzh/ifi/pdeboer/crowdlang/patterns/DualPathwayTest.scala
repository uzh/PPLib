package ch.uzh.ifi.pdeboer.crowdlang.patterns

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 16/10/14.
 */
class DualPathwayTest {
	@Test
	def testDualPathwayExecutor(): Unit = {
		val driver: TestDPDriver = new TestDPDriver
		val dpe = new DualPathwayExecutor(driver, 2)
		dpe.runUntilConverged()

		driver.data zip dpe.data.reverse forall (t => {
			val number = t._1.replaceAll("[^0-9]", "")
			Assert.assertEquals(number, t._2.answer)
			Assert.assertEquals(t._1, t._2.data)
			true
		})
	}

	private class TestDPDriver extends DPDriver {
		val data = List("test1", "test2", "test3", "test4")

		var lastIdChecked = -1

		/**
		 * return newest chunk first
		 * @param previousChunksToCheck
		 * @return
		 */
		override def processNextChunkAndReturnResult(previousChunksToCheck: List[DPChunk], newChunkElementId: Option[Int]): List[DPChunk] = {
			if (newChunkElementId.isEmpty) previousChunksToCheck
			else {
				val dataPacket: String = data(newChunkElementId.get)
				val numericVersion: String = dataPacket.replaceAll("[^0-9]", "")

				lastIdChecked = newChunkElementId.get

				DPChunk(newChunkElementId.get, dataPacket, numericVersion)() :: previousChunksToCheck
			}
		}

		override def comparePathwaysAndDecideWhetherToAdvance(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean = {
			simpleEqualityTest(pathway1, pathway2)
		}

		override def hasMoreElements(): Boolean = lastIdChecked + 1 < data.length
	}

}
