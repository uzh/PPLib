package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.patterns.ContestWithMultipleEqualWinners.PatchContainer
import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.process.{NoProcessMemoizer, ProcessMemoizer}
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.collection.parallel.{ParSet, ForkJoinTaskSupport}
import scala.util.Random
import scala.xml.NodeSeq

/**
 * Created by pdeboer on 05/12/14.
 */
@SerialVersionUID(1L) class ContestWithMultipleEqualWinners(data: List[Patch], question: HCompInstructionsWithTuple,
															title: String, findersPerItem: Int, shuffle: Boolean,
															@transient val portal: HCompPortalAdapter, maxItemsPerFind: Int = 5,
															@transient val memoizer: ProcessMemoizer = new NoProcessMemoizer(), questionAux: Option[NodeSeq] = None, pricePerVote: HCompQueryProperties = new HCompQueryProperties()
															   ) {
	val patches: List[PatchContainer] = data.map(p => new PatchContainer(p))

	def getPatches(maxToInclude: Int) = {
		val candidatePatches = patches.filter(_.displays < findersPerItem)
		val sortedPatches: List[PatchContainer] = memoizer.mem("sortedPatches")(
			if (shuffle) Random.shuffle(candidatePatches) else candidatePatches)

		sortedPatches.take(maxToInclude)
	}

	protected lazy val selectionIterations = {
		var iterations: List[List[PatchContainer]] = Nil

		while (patches.map(_.displays).sum < patches.size * findersPerItem) {
			val patches = getPatches(maxItemsPerFind)
			patches.synchronized {
				patches.foreach(p => p.displays += 1)
			}
			iterations ::= patches
		}

		iterations.reverse
	}

	protected def iteration(items: List[PatchContainer]): List[PatchContainer] = {
		val selected = askCrowdWorkers(items)
		patches.synchronized {
			selected.foreach(s => s.selects += 1)
		}
		selected
	}

	def askCrowdWorkers(items: List[PatchContainer]): List[PatchContainer] = {
		val data: List[String] = items.map(_.patch.toString)
		val orderedData = if (shuffle) Random.shuffle(data) else data

		val answer = portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(question.getInstructions("", htmlData = questionAux.getOrElse(Nil)),
				orderedData, 0, title = title + " " + Math.abs(Random.nextInt())), pricePerVote).get.asInstanceOf[MultipleChoiceAnswer]
		answer.selectedAnswers.map(a => {
			items.find(i => i.patch.toString == a).get
		})
	}

	protected lazy val selectedContainers: ParSet[PatchContainer] = {
		val it = selectionIterations.par
		it.tasksupport = new ForkJoinTaskSupport(U.hugeForkJoinPool)
		it.map(i => memoizer.mem("iteration" + i)(iteration(i))).flatten.toSet
	}

	def result = data.map(d => d -> 0).toMap ++ selectedContainers.map(p => (p.patch, p.selects)).toMap

	def selectionsPerPatch: Map[Patch, Int] = result.filter(v => v._2 > 0).toMap
}

object ContestWithMultipleEqualWinners {

	@SerialVersionUID(1l) protected[ContestWithMultipleEqualWinners] class PatchContainer(val patch: Patch, var displays: Int = 0, var selects: Int = 0) extends Serializable

}