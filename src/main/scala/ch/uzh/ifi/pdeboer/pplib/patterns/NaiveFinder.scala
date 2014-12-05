package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.recombination.entities.Patch

import scala.collection.parallel.TaskSupport
import scala.util.Random

/**
 * Created by pdeboer on 05/12/14.
 */
class NaiveFinder[T](data: List[Patch[T]], question: HCompInstructionsWithTuple, title: String, findersPerItem: Int, shuffle: Boolean, portal: HCompPortalAdapter, maxItemsPerFind: Int = 5, parallelTaskSupport: Option[TaskSupport] = None) {

	protected class PatchContainer(val patch: Patch[T], var displays: Int = 0, var selects: Int = 0)

	val patches: List[PatchContainer] = data.map(p => new PatchContainer(p))

	def getPatches(maxToInclude: Int) = {
		val candidatePatches = patches.filter(_.displays < findersPerItem)
		val sortedPatches = if (shuffle) Random.shuffle(candidatePatches) else candidatePatches

		sortedPatches.take(maxToInclude)
	}

	protected lazy val selectionIterations = {
		var iterations: List[List[PatchContainer]] = Nil

		while (patches.map(_.displays).sum < patches.size * findersPerItem) {
			val patches = getPatches(maxItemsPerFind)
			patches.foreach(p => p.displays += 1)
			iterations ::= patches
		}

		iterations.reverse
	}

	protected def iteration(items: List[PatchContainer]): List[PatchContainer] = {
		val selected = askCrowdWorkers(items)
		selected.foreach(s => s.selects += 1)
		selected
	}

	def askCrowdWorkers(items: List[PatchContainer]): List[PatchContainer] = {
		val answer = portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(question.getInstructions(""),
				items.map(_.patch.toString), 0, title = title)).get.asInstanceOf[MultipleChoiceAnswer]
		answer.selectedAnswers.map(a => {
			items.find(i => i.patch.toString == a).get
		})
	}

	protected lazy val selectedContainers = {
		val it = if (parallelTaskSupport.isDefined) {
			val it = selectionIterations.par
			it.tasksupport = parallelTaskSupport.get
			it
		} else selectionIterations
		it.map(i => iteration(i)).flatten.toSet
	}

	def result = data.map(d => d -> 0).toMap ++ selectedContainers.map(p => (p.patch, p.selects)).toMap

	def selected = result.filter(v => v._2 > 0).toMap
}
