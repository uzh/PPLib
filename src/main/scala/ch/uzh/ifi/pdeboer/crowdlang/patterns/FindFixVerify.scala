package ch.uzh.ifi.pdeboer.crowdlang.patterns

import scala.collection.mutable

/**
 * Created by pdeboer on 21/10/14.
 */
class FindFixVerifyExecutor[T](driver: FindFixVerifyDriver[T],
							   val maxPatchesCountInFind: Int = 10,
							   val findersCount: Int = 3,
							   val minFindersCountThatNeedToAgreeForFix: Int = 2,
							   val fixersPerPatch: Int = 3) {
	lazy val bestPatches = {
		if (!ran) runUntilConverged()

		allPatches.toArray.sortBy(_._1).map(p => p._2.best.getOrElse(p._2.original)).toList
	}
	protected val allPatches = driver.orderedPatches.map(p => p.patchIndex -> new FFVPatchContainer[T](p)).toMap
	private var ran: Boolean = false

	def runUntilConverged(): Unit = {
		ran = true
		val toFix = getPatchesToFix()
		val fixes = getAlternativesForPatchesToFix(toFix)
		addFixesAsAlternativesToAllPatches(fixes)

		val bestPatches = getBestPatchesFromAllPatchesVAR()
		saveBestPatchesToAllPatches()
	}

	protected def saveBestPatchesToAllPatches() {
		bestPatches.foreach(p => allPatches(p.patchIndex).best = Some(p))
	}

	protected def addFixesAsAlternativesToAllPatches(fixes: List[FFVPatch[T]]) {
		fixes.foreach(e => {
			val container = allPatches.get(e.patchIndex).get
			container.alternatives += e
		})
	}

	protected def getBestPatchesFromAllPatchesVAR(): List[FFVPatch[T]] = {
		allPatches.filter(_._2.alternatives.size > 0).map(p => driver.verify(p._2.original, p._2.alternatives.toList)).toList
	}

	protected def getAlternativesForPatchesToFix(toFix: List[FFVPatch[T]]): List[FFVPatch[T]] = {
		(1 to fixersPerPatch).par.map(i => toFix.par.map(p => driver.fix(p))).flatten.toList
	}

	protected def getPatchesToFix() = {
		var findSteps = new mutable.HashMap[Int, List[FFVPatchContainer[T]]]()
		allPatches.zipWithIndex.foreach(p => {
			val k: Int = p._2 / maxPatchesCountInFind
			val list = p._1._2 :: findSteps.getOrElse(k, List.empty[FFVPatchContainer[T]])
			findSteps += k -> list
		})

		val selectedElementsInFind = (1 to findersCount).par.map(l => findSteps.par.map(p => driver.find(p._2.map(_.original)))).flatten.flatten
		selectedElementsInFind.foreach(e => {
			val container = allPatches.get(e.patchIndex).get //should exist except if driver messes with index. out of scope for us
			container.finders += 1
		})

		allPatches.filter(_._2.finders >= findersCount).map(_._2.original).toList
	}

	protected class FFVPatchContainer[E](val original: FFVPatch[E],
										 var finders: Int = 0,
										 var alternatives: collection.mutable.ListBuffer[FFVPatch[E]] = new collection.mutable.ListBuffer[FFVPatch[E]](),
										 var best: Option[FFVPatch[E]] = None)

}

trait FindFixVerifyDriver[T] {
	def orderedPatches: List[FFVPatch[T]]

	/**
	 * given a list of patches, return all patches a single crowd worker selected as imperfect.
	 * This method will be called repeatedly
	 * @param patches
	 * @return
	 */
	def find(patches: List[FFVPatch[T]]): List[FFVPatch[T]]

	/**
	 * use a single crowd worker to fix this patch. If working with strings, you may
	 * want to show crowd workers context to that patch
	 * @param patch
	 * @return fixed version of that patch, that will be shown as an alternative in the next step
	 */
	def fix(patch: FFVPatch[T]): FFVPatch[T]

	/**
	 * all alternatives collected for a single patch in the previous step are evaluated here
	 * and a best version is selected. Consider banning people who participated in the "Fix"-step
	 * from this step.
	 * @param patch
	 * @param alternatives
	 */
	def verify(patch: FFVPatch[T], alternatives: List[FFVPatch[T]]): FFVPatch[T]
}

case class FFVPatch[T](patch: T, patchIndex: Int)
