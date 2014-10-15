package ch.uzh.ifi.pdeboer.crowdlang.patterns

/**
 * Created by pdeboer on 13/10/14.
 */
//TODO code me
class DualPathwayExecutor(driver: DPDriver, chunkCountToInclude: Int = 2) {
	private val pathway1 = new DPPathway(List.empty)
	private val pathway2 = new DPPathway(List.empty)

	private def step(pathway: DPPathway): Unit = {
		if (pathway1.elements.isEmpty) init()

		val previousChunks = pathway.elements.take(chunkCountToInclude).toList
		val newPreviousChunks = driver.processNextChunkAndReturnResult(previousChunks)

		//TODO sync

		//TODO advance
	}

	private def init() {
		val elem1 = driver.processNextChunkAndReturnResult(List.empty[DPChunk])(0)
		val elem2 = driver.processNextChunkAndReturnResult(List.empty[DPChunk])(0)

		List(pathway1, pathway2).foreach(p => {
			p.elements = elem1 :: p.elements
			p.elements = elem2 :: p.elements
		})
	}
}

class DPPathway(var elements: List[DPChunk])

trait DPDriver {
	def processNextChunkAndReturnResult(previousChunksToCheck: List[DPChunk]): List[DPChunk]

	def comparePathwaysAndDecideWhetherToAdvance(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean
}

case class DPChunk(chunk: String)(answer: String = "")