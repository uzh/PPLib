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
				driver.elementIndexExists(pathway1.mostRecentElementIdInPathway + 1)
		) {
			step(pathway1)
			step(pathway2)
		}
	}

	protected def step(pathway: DPPathway) = {
		if (pathway1.elements.isEmpty) init()

		val pathwayChunks = pathway.getNElements(chunkCountToInclude)

		val previousChunks = pathwayChunks.map(_.mostRecentCandidate).toList
		val nextIndexToProcess: Int = pathway.mostRecentElementIdInPathway + 1
		val newElementToAddInThisRound: Boolean = advancementAllowed(pathway) && driver.elementIndexExists(nextIndexToProcess)
		val updatedPreviousChunks = driver.processNextChunkAndReturnResult(previousChunks,
			if (newElementToAddInThisRound) Some(nextIndexToProcess) else None)

		val numberOfNewElements: Int = if (newElementToAddInThisRound) 1 else 0
		val expectedLength = previousChunks.length + numberOfNewElements

		if (updatedPreviousChunks.size != expectedLength) throw new IllegalArgumentException("unexpected answer")
		else {
			//add updated elements
			updatedPreviousChunks.drop(numberOfNewElements).zip(pathwayChunks).foreach(t => t._2.addMostRecentCandidate(t._1))

			//add new element
			if (newElementToAddInThisRound) {
				pathway.addElement(new DPPathwayChunk(updatedPreviousChunks(0)))
			}

			advancementAllowed += (pathway -> false)
			val otherPathway = if (pathway == pathway1) pathway2 else pathway1

			if (otherPathway.mostRecentElementIdInPathway == pathway.mostRecentElementIdInPathway) {
				val pathwaysAreEqual = driver.comparePathwaysAndDecideWhetherToAdvance(
					pathway.getNElementsPayload(chunkCountToInclude),
					otherPathway.getNElementsPayload(chunkCountToInclude))

				advancementAllowed += (pathway -> pathwaysAreEqual)
				advancementAllowed += (otherPathway -> pathwaysAreEqual)
			}
		}
	}

	protected def init() {
		val elems = (0 to chunkCountToInclude - 1).map(i => driver.processNextChunkAndReturnResult(List.empty[DPChunk], Some(i))(0))

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
		if (!candidates.forall(_.elementIndex == chunk.elementIndex)) throw new IllegalArgumentException("indices unaligned")
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

	def elementIndexExists(index: Int): Boolean

	def simpleEqualityTest(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean = {
		pathway1 zip pathway2 forall (t => t._1 equals t._2)
	}
}

case class DPChunk(elementIndex: Int, data: String, answer: String = "")(val created: Date = new Date(), var aux: String = "")