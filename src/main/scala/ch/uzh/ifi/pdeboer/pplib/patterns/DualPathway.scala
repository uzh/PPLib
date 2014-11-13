package ch.uzh.ifi.pdeboer.pplib.patterns

import java.util.Date

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._

/**
 * Created by pdeboer on 13/10/14.
 */
class DualPathwayExecutor(driver: DPDriver, chunkCountToInclude: Int = 2) extends LazyLogging {
	lazy val data = {
		runUntilConverged()
		pathway1.elements.map(_.mostRecentCandidate).toList
	}
	protected val pathway1 = new DPPathway()
	protected val pathway2 = new DPPathway()
	protected var advancementAllowed = Map(pathway1 -> true, pathway2 -> true)

	def runUntilConverged(): Unit = {
		logger.info("running dual pathway until convergence")
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
		logger.info("executing dual-pathway step. current pathway state: " + pathway)

		if (pathway1.elements.isEmpty) init()

		val pathwayChunks = pathway.getNElements(chunkCountToInclude)

		val previousChunks = pathwayChunks.map(_.mostRecentCandidate).toList
		val nextIndexToProcess: Int = pathway.mostRecentElementIdInPathway + 1
		val newElementToAddInThisRound: Boolean = advancementAllowed(pathway) && driver.elementIndexExists(nextIndexToProcess)
		val updatedPreviousChunks = driver.processChunksAndPossiblyAddNew(previousChunks,
			if (newElementToAddInThisRound) Some(nextIndexToProcess) else None).sortBy(-_.elementIndex)

		val numberOfNewElements: Int = if (newElementToAddInThisRound) 1 else 0
		val expectedLength = previousChunks.length + numberOfNewElements

		if (updatedPreviousChunks.size != expectedLength) throw new IllegalArgumentException("unexpected length of answer: should be " + expectedLength + " is " + updatedPreviousChunks.size)
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
		val elems = (0 to chunkCountToInclude - 1).map(i => driver.processChunksAndPossiblyAddNew(List.empty[DPChunk], Some(i))(0))

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

	override def toString: String = _elements.mkString(",")
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

	override def toString: String = mostRecentCandidate.toString
}

trait DPDriver {
	def processChunksAndPossiblyAddNew(previousChunksToCheck: List[DPChunk], newChunkElementId: Option[Int] = None): List[DPChunk]

	def comparePathwaysAndDecideWhetherToAdvance(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean

	def elementIndexExists(index: Int): Boolean

	def simpleEqualityTest(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean = {
		pathway1 zip pathway2 forall (t => t._1 equals t._2)
	}
}

case class DPChunk(elementIndex: Int, data: String, var answer: String = "", var aux: String = "") {
	val created: Date = new Date()

	override def toString: String = s"$data=$answer"
}


class DualPathWayDefaultHCompDriver(
									   val data: List[String],
									   val portal: HCompPortalAdapter,
									   val questionPerOldProcessedElement: HCompInstructionsWithTuple,
									   val questionPerNewProcessedElement: HCompInstructionsWithTuple,
									   val questionPerProcessingTask: String,
									   val questionPerComparisonTask: DPHCompDriverDefaultComparisonInstructionsConfig,
									   val timeout: Duration = 2 days) extends DPDriver {

	lazy val indexMap = data.zipWithIndex.map(d => (d._2, d._1)).toMap

	/**
	 * return newest chunk first
	 * @param previousChunksToCheck
	 * @return
	 */
	override def processChunksAndPossiblyAddNew(previousChunksToCheck: List[DPChunk], newChunkElementId: Option[Int] = None): List[DPChunk] = {
		val previousQueries = previousChunksToCheck.map(c => new DPFreetextQuery(questionPerOldProcessedElement.getInstructions(c.data, c.answer), c.answer, c)).toList
		val newQuery = if (newChunkElementId.isDefined) {
			val c = DPChunk(newChunkElementId.get, indexMap(newChunkElementId.get))
			Some(List(new DPFreetextQuery(questionPerNewProcessedElement.getInstructions(c.data), "", c)))
		} else None

		val composite = CompositeQuery(newQuery.getOrElse(Nil) ::: previousQueries, questionPerProcessingTask)
		val res = portal.sendQueryAndAwaitResult(composite, maxWaitTime = timeout)

		val answer = res.get.asInstanceOf[CompositeQueryAnswer]
		answer.answers.map(t => {
			val chunk: DPChunk = t._1.asInstanceOf[DPFreetextQuery].chunk
			chunk.answer = t._2.get.asInstanceOf[FreetextAnswer].answer
			chunk
		}).toList
	}

	override def comparePathwaysAndDecideWhetherToAdvance(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean = {
		//TODO currently issues single request. We might want to allow for other mechanisms of consent
		val POSITIVE_ANSWER: String = "Yes"
		val res = portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(questionPerComparisonTask.getQuestion(pathway1, pathway2),
				List(POSITIVE_ANSWER, "No"), 1, 1),
			maxWaitTime = timeout)
		res.get.asInstanceOf[MultipleChoiceAnswer].selectedAnswer == POSITIVE_ANSWER
	}

	override def elementIndexExists(index: Int): Boolean = indexMap.contains(index)

	private class DPFreetextQuery(queryQuestion: String, queryDefaultAnswer: String, val chunk: DPChunk) extends FreetextQuery(queryQuestion, queryDefaultAnswer) {}

}

class DPHCompDriverDefaultComparisonInstructionsConfig(val title: String = "Comparison",
													   val preText: String = "Please compare both pathways and answer if the answers are equal or not",
													   val questionTitle: String = "Question",
													   val leftTitle: String = "Pathway 1",
													   val rightTitle: String = "Pathway 2") {
	def getQuestion(left: List[DPChunk], right: List[DPChunk]): String = <div>
		<h1>
			{title}
		</h1>{preText}<table>
			<tr>
				<td>
					{questionTitle}
				</td>
				<td>
					{leftTitle}
				</td>
				<td>
					{rightTitle}
				</td>
			</tr>{left.zip(right).map(lr => {
				<tr>
					<td>
						{lr._1.data}
					</td>
					<td>
						{lr._1.answer}
					</td>
					<td>
						{lr._2.answer}
					</td>
				</tr>
			})}
		</table>
	</div>.toString()
}