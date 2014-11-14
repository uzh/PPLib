package ch.uzh.ifi.pdeboer.pplib.patterns

import com.typesafe.scalalogging.LazyLogging
import org.junit.{Assert, Test}

import scala.util.Random

/**
 * Created by pdeboer on 22/10/14.
 */
class FindFixVerifyTest extends LazyLogging {
	@Test
	def testFFVDetailed(): Unit = {
		val (badPatches: List[FFVTestDriverBadPatch], exec: FindFixVerifyTestVisibilityBreaker) = prepareData

		val toFix = exec.getPatchesToFix()
		Assert.assertEquals("finds errorous", badPatches.map(_.original).toSet, toFix.toSet)

		val fixes = exec.getAlternativesForPatchesToFix(toFix)
		Assert.assertEquals("fixes erroneous", badPatches.map(_.alternatives).flatten.sortBy(_.patch), fixes.sortBy(_.patch))

		exec.addFixesAsAlternativesToAllPatches(fixes)
		val bestPatches = exec.getBestPatchesFromAllPatchesVAR()
		Assert.assertEquals("verify errorous", badPatches.map(_.best).toSet, bestPatches.toSet)
	}

	@Test
	def testFFVOneShot(): Unit = {
		val (badPatches: List[FFVTestDriverBadPatch], exec: FindFixVerifyTestVisibilityBreaker) = prepareData

		retryUntilItWorked() {
			checkOneShot(badPatches.map(_.original), badPatches, exec)
		}
	}

	private def retryUntilItWorked(numTries: Int = 20)(m: => Boolean): Unit = {
		Assert.assertTrue((1 to numTries).exists(i => m))
	}

	private def prepareData: (List[FFVTestDriverBadPatch], FindFixVerifyTestVisibilityBreaker) = {
		val dataSet = (1 to 20).map(i => ("test" + i, i)).map(t => new FFVPatch[String](t._1, t._2)).toList
		val badPatches = dataSet.map(d => new FFVTestDriverBadPatch(d,
			(1 to 3).map(i => new FFVPatch[String](d.patch + i, d.patchIndex)).toList,
			new FFVPatch[String](d.patch + 2, d.patchIndex)
		))

		val driver = new FFVTestDriver(dataSet, badPatches, 10)

		val exec = new FindFixVerifyTestVisibilityBreaker(driver)
		(badPatches, exec)
	}

	@Test
	def testFFVOnlyFewFixes(): Unit = {
		val dataSet = (1 to 20).map(i => ("test" + i, i)).map(t => new FFVPatch[String](t._1, t._2)).toList
		val patchesToCreateBadPatchesFor = dataSet.map(d => (d, new Random().nextDouble())).sortBy(_._2).take(10).map(_._1)
		val badPatches = patchesToCreateBadPatchesFor.map(p => new FFVTestDriverBadPatch(
			p,
			(1 to 3).map(i => new FFVPatch[String](p.patch + i, p.patchIndex)).toList,
			new FFVPatch[String](p.patch + 2, p.patchIndex)
		))

		//we need to return at least 7 patches in find-step such that we return 3*7=21 patches
		// in total, which basically means 2 in 3 turkers agreeing that this patch is not
		// optimal
		val exec: FindFixVerifyTestVisibilityBreaker = new FindFixVerifyTestVisibilityBreaker(new FFVTestDriver(dataSet, badPatches, 7))
		retryUntilItWorked() {
			checkOneShot(dataSet, badPatches, exec)
		}
	}

	def checkOneShot(dataSet: List[FFVPatch[String]], badPatches: List[FFVTestDriverBadPatch], exec: FindFixVerifyTestVisibilityBreaker) = {
		logger.info("running one-shot")
		val badPatchesWithoutInfo = badPatches.map(_.original).toSet
		val badPatchesInclBest = dataSet.filterNot(d => badPatchesWithoutInfo.contains(d)) ::: badPatches.map(_.best)


		badPatchesInclBest.toSet.equals(exec.bestPatches.toSet)
	}

	private class FFVTestDriverBadPatch(var original: FFVPatch[String], val alternatives: List[FFVPatch[String]], val best: FFVPatch[String], var remainingFinds: Int = 3) {
		var remainingAlternatives: List[FFVPatch[String]] = alternatives
	}

	private class FFVTestDriver(val orderedPatches: List[FFVPatch[String]], badPatches: List[FFVTestDriverBadPatch], numberOfPatchesReturnedInFind: Int = 3) extends FindFixVerifyDriver[String] {
		/**
		 * all alternatives collected for a single patch in the previous step are evaluated here
		 * and a best version is selected. Consider banning people who participated in the "Fix"-step
		 * from this step.
		 * @param patch
		 * @param alternatives
		 */
		override def verify(patch: FFVPatch[String], alternatives: List[FFVPatch[String]]): FFVPatch[String] = {
			val badPatch = badPatches.find(_.original == patch).get
			badPatch.best
		}

		/**
		 * use a single crowd worker to fix this patch. If working with strings, you may
		 * want to show crowd workers context to that patch
		 * @param patch
		 * @return fixed version of that patch, that will be shown as an alternative in the next step
		 */
		override def fix(patch: FFVPatch[String]): FFVPatch[String] = {
			badPatches.synchronized {
				val badPatch = badPatches.find(_.original == patch).get
				val ret = badPatch.remainingAlternatives.head
				badPatch.remainingAlternatives = badPatch.remainingAlternatives.drop(1)
				ret
			}
		}

		/**
		 * given a list of patches, return all patches a single crowd worker selected as imperfect.
		 * This method will be called repeatedly
		 * @param patches
		 * @return
		 */
		override def find(patches: List[FFVPatch[String]]): List[FFVPatch[String]] = {
			badPatches.synchronized {
				val candidates = patches.filter(p => badPatches.exists(b => b.original == p && b.remainingFinds > 0))

				val randomlySelectedCandidates = candidates.map(c => (c, new Random().nextDouble())).sortBy(_._2).take(numberOfPatchesReturnedInFind)
				randomlySelectedCandidates.foreach(c => badPatches.find(b => b.original == c._1).get.remainingFinds -= 1)
				randomlySelectedCandidates.map(_._1)
			}
		}
	}

	private class FindFixVerifyTestVisibilityBreaker(driver: FindFixVerifyDriver[String],
													 maxPatchesCountInFind: Int = 10,
													 findersCount: Int = 3,
													 minFindersCountThatNeedToAgreeForFix: Int = 2,
													 fixersPerPatch: Int = 3) extends FindFixVerifyExecutor[String](driver, maxPatchesCountInFind, findersCount, minFindersCountThatNeedToAgreeForFix, fixersPerPatch) {
		def allPatchesVal = allPatches

		override def getBestPatchesFromAllPatchesVAR(): List[FFVPatch[String]] = super.getBestPatchesFromAllPatchesVAR()

		override def getAlternativesForPatchesToFix(toFix: List[FFVPatch[String]]): List[FFVPatch[String]] = super.getAlternativesForPatchesToFix(toFix)

		override def getPatchesToFix(): List[FFVPatch[String]] = super.getPatchesToFix()

		override def addFixesAsAlternativesToAllPatches(fixes: List[FFVPatch[String]]): Unit = super.addFixesAsAlternativesToAllPatches(fixes)

		override protected def saveBestPatchesToAllPatches(bestPatchesFound: List[FFVPatch[String]]): Unit = super.saveBestPatchesToAllPatches(bestPatchesFound)
	}

}
