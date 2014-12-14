package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.process.entities.{StringWrapper, Patch}
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 14/12/14.
 */
class FixPatchTest {
	val (plannedQuestion: Patch, plannedAnswer: Patch) = (new Patch(new StringWrapper("hallo")), new Patch(new StringWrapper("hallo2")))

	@Test
	def testFixForPatchAtIndex: Unit = {
		val allPatchesBase: List[Patch] = List("a", "b", "c", "d").map(p => new Patch(new StringWrapper(p)))
		val allPatches: List[Patch] = allPatchesBase.take(2) ::: List(plannedQuestion) ::: allPatchesBase.takeRight(2)

		val driver = new FixPatchTrivialDriver(Map(plannedQuestion -> plannedAnswer))
		val exec = new FixPatchExecuter(driver, allPatches, List(2), (2, 2))

		val answer = exec.getFixForPatchAtIndex(2)
		Assert.assertEquals(plannedAnswer, answer)
		Assert.assertEquals(AskedQuestions(plannedQuestion, allPatchesBase.take(2), allPatchesBase.takeRight(2)), driver.questions(0))
	}


	@Test
	def testFixForPatchAtIndexWithNotEnoughTrailingAndLeading: Unit = {
		val allPatchesBase: List[Patch] = List("a", "b", "c", "d").map(p => new Patch(new StringWrapper(p)))
		val allPatches: List[Patch] = allPatchesBase.take(2) ::: List(plannedQuestion) ::: allPatchesBase.takeRight(2)

		val driver = new FixPatchTrivialDriver(Map(plannedQuestion -> plannedAnswer))
		val exec = new FixPatchExecuter(driver, allPatches, List(2), (3, 3))

		val answer = exec.getFixForPatchAtIndex(2)
		Assert.assertEquals(plannedAnswer, answer)
		Assert.assertEquals(AskedQuestions(plannedQuestion, allPatchesBase.take(2), allPatchesBase.takeRight(2)), driver.questions(0))
	}


	@Test
	def testFixForPatchAtIndexWithMoreThanEnough: Unit = {
		val allPatchesBase: List[Patch] = List("a", "b", "c", "d").map(p => new Patch(new StringWrapper(p)))
		val allPatches: List[Patch] = allPatchesBase.take(2) ::: List(plannedQuestion) ::: allPatchesBase.takeRight(2)

		val driver = new FixPatchTrivialDriver(Map(plannedQuestion -> plannedAnswer))
		val exec = new FixPatchExecuter(driver, allPatches, List(2), (1, 1))

		val answer = exec.getFixForPatchAtIndex(2)
		Assert.assertEquals(plannedAnswer, answer)
		Assert.assertEquals(AskedQuestions(plannedQuestion, List(allPatchesBase(1)), List(allPatchesBase(2))), driver.questions(0))
	}

	private class FixPatchTrivialDriver(answers: Map[Patch, Patch] = Map.empty) extends FixPatchDriver {
		var questions: List[AskedQuestions] = Nil

		override def fix(patch: Patch, patchesBefore: List[Patch], patchesAfterwards: List[Patch]): Patch = {
			questions = AskedQuestions(patch, patchesBefore, patchesAfterwards) :: questions
			answers(patch)
		}
	}

	private case class AskedQuestions(patch: Patch, patchesBefore: List[Patch], patchesAfterwards: List[Patch])

}
