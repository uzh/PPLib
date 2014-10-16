package ch.uzh.ifi.pdeboer.crowdlang.patterns

import java.util.Date

/**
 * Created by pdeboer on 13/10/14.
 */
class DualPathwayExecutor(driver: DPDriver, chunkCountToInclude: Int = 2) {
	lazy val data = {
		runUntilConverged()
		pathway1.elements.map(_.mostRecentCandidate).toList
	}
	protected val pathway1 = new DPPathway()
	protected val pathway2 = new DPPathway()
	protected var advancementAllowed = Map(pathway1 -> true, pathway2 -> true)

	def runUntilConverged(): Unit = {
		while (
			pathway1.mostRecentElementIdInPathway == -1 ||
				pathway1.mostRecentElementIdInPathway != pathway2.mostRecentElementIdInPathway ||
				advancementAllowed.values.exists(_ == false) ||
				driver.hasMoreElements()
		) {
			step(pathway1)
			step(pathway2)
		}
	}

	protected def step(pathway: DPPathway): Boolean = {
		if (pathway1.elements.isEmpty) init()

		val pathwayChunks = pathway.getNElements(chunkCountToInclude)

		val previousChunks = pathwayChunks.map(_.mostRecentCandidate).toList
		val updatedPreviousChunks = driver.processNextChunkAndReturnResult(previousChunks,
			if (advancementAllowed(pathway)) Some(pathway.mostRecentElementIdInPathway + 1) else None)

		if (updatedPreviousChunks.size != pathwayChunks.size) false
		else {
			pathwayChunks zip updatedPreviousChunks foreach (t => t._1.addMostRecentCandidate(t._2))
			val otherPathway = if (pathway == pathway1) pathway2 else pathway1
			val minElementIdInList = updatedPreviousChunks.minBy(_.elementIndex).elementIndex

			val pathwaysAreEqual = driver.comparePathwaysAndDecideWhetherToAdvance(
				pathway.getNElementsPayload(chunkCountToInclude),
				otherPathway.elements.toStream //get only elements with the same index range
					.map(_.mostRecentCandidate)
					.dropWhile(_.elementIndex > pathway.mostRecentElementIdInPathway)
					.takeWhile(_.elementIndex <= minElementIdInList).toList)

			advancementAllowed += (pathway -> pathwaysAreEqual)
			true
		}
	}

	protected def init() {
		val elems = (1 to chunkCountToInclude).map(i => driver.processNextChunkAndReturnResult(List.empty[DPChunk], Some(i))(0))

		List(pathway1, pathway2).foreach(p => {
			elems.foreach(e => p.addElement(new DPPathwayChunk(e)))
		})
	}
}

class DPPathway() {
	private var _elements: List[DPPathwayChunk] = List.empty

	def addElement(elem: DPPathwayChunk): Unit = {
		_elements = elem :: _elements
	}

	def mostRecentElementIdInPathway = if (_elements.length == 0) -1 else _elements(0).mostRecentCandidate.elementIndex

	def getNElementsPayload(n: Int) = getNElements(n).map(_.mostRecentCandidate).toList

	def getNElements(n: Int) = _elements.take(n).toList

	def elements = _elements
}

class DPPathwayChunk(initialChunk: DPChunk) {
	private var candidates: List[DPChunk] = List.empty[DPChunk]

	addMostRecentCandidate(initialChunk)

	def addMostRecentCandidate(chunk: DPChunk): Unit = {
		candidates = chunk :: candidates
	}

	def mostRecentCandidate = candidates(0)

	def allCandidates = candidates.toList
}

trait DPDriver {
	/**
	 * return newest chunk first
	 * @param previousChunksToCheck
	 * @return
	 */
	def processNextChunkAndReturnResult(previousChunksToCheck: List[DPChunk], newChunkElementId: Option[Int] = None): List[DPChunk]

	def comparePathwaysAndDecideWhetherToAdvance(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean

	def hasMoreElements(): Boolean

	def simpleEqualityTest(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean = {
		pathway1 zip pathway2 forall (t => t._1 equals t._2)
	}
}

case class DPChunk(elementIndex: Int, data: String, answer: String = "")(created: Date = new Date(), var aux: String = "")