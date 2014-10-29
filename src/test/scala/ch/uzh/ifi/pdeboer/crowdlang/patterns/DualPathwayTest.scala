package ch.uzh.ifi.pdeboer.crowdlang.patterns

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 16/10/14.
 */
class DualPathwayTest {
	@Test
	def testDualPathwayExecutorAllGood(): Unit = {
		val driver: TestDPDriver = new TestDPDriver
		runTest(driver)
	}

	@Test
	def testDualPathwayExecutorOneErrorAfterInit(): Unit = {
		val driver: TestDPDriver = new TestDPDriver(1)
		runTest(driver)
	}

	@Test
	def testDualPathwayExecutorOneErrorInMiddle(): Unit = {
		val driver: TestDPDriver = new TestDPDriver(2)
		runTest(driver)
	}

	@Test
	def testDualPathwayExecutorOneErrorAtEnd(): Unit = {
		val driver: TestDPDriver = new TestDPDriver(3)
		runTest(driver)
	}

	@Test
	def testDualPathwayExecutorOneErrorAfterInitUnevenPathway(): Unit = {
		val driver: TestDPDriver = new TestDPDriver(1, false)
		runTest(driver)
	}

	@Test
	def testDualPathwayExecutorOneErrorInMiddleUnevenPathway(): Unit = {
		val driver: TestDPDriver = new TestDPDriver(2, false)
		runTest(driver)
	}

	def runTest(driver: TestDPDriver): Unit = {
		val dpe = new DualPathwayExecutor(driver, 2)
		dpe.runUntilConverged()

		driver.data zip dpe.data.reverse forall (t => {
			val number = t._1.replaceAll("[^0-9]", "")
			Assert.assertEquals(number, t._2.answer)
			Assert.assertEquals(t._1, t._2.data)
			true
		})
	}

	@Test
	def testDualPathwayExecutorOneErrorAtEndUnevenPathway(): Unit = {
		val driver: TestDPDriver = new TestDPDriver(3, false)
		runTest(driver)
	}

	private class TestDPDriver(introduceErrorAtStepId: Int = -1, introduceIntoFirstPathway: Boolean = true) extends DPDriver {
		val data = List("test1", "test2", "test3", "test4")
		var errorIntroduced: Boolean = false
		var errorIntroducedAndCheckDone: Boolean = false

		var lastStepId: Int = -1

		/**
		 * return newest chunk first
		 * @param previousChunksToCheck
		 * @return
		 */
		override def processChunksAndPossiblyAddNew(previousChunksToCheck: List[DPChunk], newChunkElementId: Option[Int]): List[DPChunk] = {
			Thread.sleep(10)

			val fixedPrevChunks: List[DPChunk] = previousChunksToCheck.map(c => {
				DPChunk(c.elementIndex, c.data, c.data.replaceAll("[^0-9]", ""))()
			}).toList

			if (newChunkElementId.isEmpty) {
				fixedPrevChunks
			}
			else {
				val dataPacket: String = data(newChunkElementId.get)
				val answer: String = if (newChunkElementId.getOrElse(-1) == introduceErrorAtStepId && !errorIntroduced
					&& (introduceIntoFirstPathway || !introduceIntoFirstPathway && lastStepId == newChunkElementId.getOrElse(-1))) {
					errorIntroduced = true
					"bla"
				} else dataPacket.replaceAll("[^0-9]", "")

				lastStepId = newChunkElementId.get

				DPChunk(newChunkElementId.get, dataPacket, answer)() :: fixedPrevChunks
			}
		}

		override def comparePathwaysAndDecideWhetherToAdvance(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean = {
			val isErrorComparison = errorIntroduced && !errorIntroducedAndCheckDone
			if (isErrorComparison) {
				this.errorIntroducedAndCheckDone = true
			}
			simpleEqualityTest(pathway1, pathway2)
		}

		override def elementIndexExists(index: Int): Boolean = index > -1 && index < data.length
	}

}
