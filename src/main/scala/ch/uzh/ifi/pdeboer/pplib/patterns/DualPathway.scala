package ch.uzh.ifi.pdeboer.pplib.patterns

import java.util.Date

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.entities.IndexedPatch
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

import scala.concurrent.duration._
import scala.util.Random
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 13/10/14.
 */
@SerialVersionUID(1l)
class DualPathwayExecutor(_driver: DPDriver, chunkCountToInclude: Int = 2) extends LazyLogger with Serializable {
	@transient var driver = _driver

	lazy val result = {
		runUntilConverged()
		pathway1.elements.map(_.mostRecentCandidate).toList
	}
	protected val pathway1 = new DPPathway()
	protected val pathway2 = new DPPathway()
	protected var advancementAllowed = Map(pathway1 -> true, pathway2 -> true)

	def runUntilConverged(): Unit = {
		logger.info("running dual pathway until convergence")
		var iteration = 0
		while (
			pathway1.mostRecentElementIdInPathway == -1 ||
				pathway1.mostRecentElementIdInPathway != pathway2.mostRecentElementIdInPathway ||
				advancementAllowed.values.exists(_ == false) ||
				driver.elementIndexExists(pathway1.mostRecentElementIdInPathway + 1)
		) {
			logger.info("executing step in pathway 1")
			step(pathway1)
			logger.info("executing step in pathway 2")
			step(pathway2)
		}
	}

	protected def step(pathway: DPPathway) = {
		logger.debug("executing dual-pathway step. current pathway state: " + pathway)

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

@SerialVersionUID(1l)
class DPPathway extends Serializable {
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

@SerialVersionUID(1l)
class DPPathwayChunk(initialChunk: DPChunk) extends Serializable {
	private var candidates: List[DPChunk] = List.empty[DPChunk]

	addMostRecentCandidate(initialChunk)

	def addMostRecentCandidate(chunk: DPChunk): Unit = {
		if (!candidates.forall(_.elementIndex == chunk.elementIndex))
			throw new IllegalArgumentException("indices unaligned")
		candidates = chunk :: candidates
	}

	def mostRecentCandidate = candidates(0)

	def allCandidates = candidates.toList

	override def toString: String = mostRecentCandidate.toString
}

@SerialVersionUID(1l) trait DPDriver extends Serializable {
	def processChunksAndPossiblyAddNew(previousChunksToCheck: List[DPChunk], newChunkElementId: Option[Int] = None): List[DPChunk]

	def comparePathwaysAndDecideWhetherToAdvance(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean

	def elementIndexExists(index: Int): Boolean

	def simpleEqualityTest(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean = {
		pathway1 zip pathway2 forall (t => t._1 equals t._2)
	}
}

@SerialVersionUID(1l) case class DPChunk(elementIndex: Int, data: IndexedPatch, var answer: String = "", var aux: String = "") extends Serializable {
	val created: Date = new Date()

	override def toString: String = s"$data=$answer"

	def answerAsPatch = data.duplicate(answer).asInstanceOf[IndexedPatch]
}


@SerialVersionUID(1l) class DualPathWayDefaultHCompDriver(
															 val data: List[IndexedPatch],
															 val portal: HCompPortalAdapter,
															 val questionPerOldProcessedElement: HCompInstructionsWithTupleStringified,
															 val questionPerNewProcessedElement: HCompInstructionsWithTupleStringified,
															 val questionPerProcessingTask: String,
															 val questionPerComparisonTask: DPHCompDriverDefaultComparisonInstructionsConfig,
															 val timeout: Duration = 14 days) extends DPDriver with Serializable {

	lazy val indexMap: Map[Int, IndexedPatch] = data.map(d => (d.index, d)).toMap

	/**
	 * return newest chunk first
	 * @param previousChunksToCheck
	 * @return
	 */
	override def processChunksAndPossiblyAddNew(previousChunksToCheck: List[DPChunk], newChunkElementId: Option[Int] = None): List[DPChunk] = {
		val previousQueries = previousChunksToCheck.map(c => new DPFreetextQuery(questionPerOldProcessedElement.getInstructions(c.data.value, c.answer), c.answer, c)).toList
		val newQuery = if (newChunkElementId.isDefined) {
			val c = DPChunk(newChunkElementId.get, indexMap(newChunkElementId.get))
			Some(List(new DPFreetextQuery(
				questionPerNewProcessedElement.getInstructions(c.data.value), "", c)))
		} else None

		val composite = CompositeQuery(
			newQuery.getOrElse(Nil) ::: previousQueries, questionPerProcessingTask, questionPerProcessingTask)
		val res = portal.sendQueryAndAwaitResult(
			composite, maxWaitTime = timeout, properties = HCompQueryProperties(6))

		val answer = res.get.asInstanceOf[CompositeQueryAnswer]
		answer.answers.map(t => {
			val chunk: DPChunk = t._1.asInstanceOf[DPFreetextQuery].chunk
			chunk.answer = t._2.get.asInstanceOf[FreetextAnswer].answer
			chunk
		}).toList
	}

	override def comparePathwaysAndDecideWhetherToAdvance(pathway1: List[DPChunk], pathway2: List[DPChunk]): Boolean = {
		val alternatives: List[String] = List(questionPerComparisonTask.positiveAnswerForComparison, questionPerComparisonTask.negativeAnswerForComparison)
		val choices = if (questionPerComparisonTask.shuffleChoices) Random.shuffle(alternatives) else alternatives

		//TODO currently issues single request. We might want to allow for other mechanisms of consent
		val res = portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(questionPerComparisonTask.getQuestion(pathway1, pathway2),
				choices, 1, 1, title = questionPerComparisonTask.title),
			maxWaitTime = timeout,
			properties = HCompQueryProperties(2)
		)
		res.get.asInstanceOf[MultipleChoiceAnswer].selectedAnswer == questionPerComparisonTask.positiveAnswerForComparison
	}

	override def elementIndexExists(index: Int): Boolean = indexMap.contains(index)

	private class DPFreetextQuery(queryQuestion: String, queryDefaultAnswer: String, val chunk: DPChunk) extends FreetextQuery(queryQuestion, queryDefaultAnswer) {}

}

class DPHCompDriverDefaultComparisonInstructionsConfig(
														  val title: String = "Comparison",
														  val preText: String = "Please compare both solutions and check if they are equal or not. (They don't need to have the exact same characters; much rather they should be equal in quality)",
														  val postText: String = "",
														  val questionTitle: String = "Question",
														  val leftTitle: String = "Solution 1",
														  val rightTitle: String = "Solution 2",
														  val positiveAnswerForComparison: String = "Yes, they are equal",
														  val negativeAnswerForComparison: String = "No, they are NOT equal",
														  val shuffleChoices: Boolean = true) {
	def getQuestion(left: List[DPChunk], right: List[DPChunk]): String = NodeSeq.fromSeq(<div>
		<h1>
			{title}
		</h1>{preText}<table>
			<tr>
				<th>
					{questionTitle}
				</th>
				<th>
					{leftTitle}
				</th>
				<th>
					{rightTitle}
				</th>
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
	</div>.child).toString
}