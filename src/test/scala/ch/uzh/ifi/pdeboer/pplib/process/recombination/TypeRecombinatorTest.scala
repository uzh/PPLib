package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib._
import ch.uzh.ifi.pdeboer.pplib.util.TestUtils
import org.junit.{Assert, Test}

import scala.reflect.runtime.universe._

/**
 * Created by pdeboer on 27/03/15.
 */
class TypeRecombinatorTest {
	TestUtils.ensureThereIsAtLeast1Portal()

	def newDB = {
		val db = new RecombinationDB
		db.addClass(classOf[Collection])
		db.addClass(classOf[CollectionWithSigmaPruning])
		db
	}

	@Test
	def testFixProcessRecombination: Unit = {
		val db = new RecombinationDB
		db.addClass(classOf[FixPatchProcess])
		db.addClass(classOf[FindFixPatchProcess])
		db.addClass(classOf[Collection])
		db.addClass(classOf[ContestWithMultipleEqualWinnersProcess])
		db.addClass(classOf[CollectDecideProcess])
		db.addClass(classOf[Contest])

		val recombinator = new TypeRecombinator(RecombinationHints.create(TypeRecombinatorTest.DEFAULT_TESTING_HINTS), db)
		val results = recombinator.materialize[FindFixPatchProcess]

		Assert.assertEquals(1, results.size)
	}

	@Test
	def testApplicableTypes: Unit = {
		val db = new RecombinationDB
		class ApplicableType extends CreateProcess[List[IndexedPatch], List[IndexedPatch]](Map.empty) {
			override protected def run(data: List[IndexedPatch]): List[IndexedPatch] = Nil
		}
		db.addClass(classOf[ApplicableType])

		val recombinator = new TypeRecombinator(RecombinationHints.create(Map.empty), db)

		Assert.assertEquals("default case", 1, recombinator.getApplicableTypesInDB(typeOf[CreateProcess[List[IndexedPatch], List[IndexedPatch]]]).size)
		Assert.assertEquals("generic superclass", 1, recombinator.getApplicableTypesInDB(typeOf[CreateProcess[_ <: List[Patch], _ <: List[Patch]]]).size)
	}

	@Test
	def testTrivialMaterialize: Unit = {
		val db = newDB
		val r = new TypeRecombinator(RecombinationHints.create(Map()), db)
		val materialized = r.materialize[CreateProcess[Patch, List[Patch]]]

		val processClasses = materialized.map(_.clazz).toSet
		Assert.assertEquals(Set(classOf[Collection], classOf[CollectionWithSigmaPruning]), processClasses)
	}

	@Test
	def testTypeConstrainedMaterialize: Unit = {
		val db = newDB
		db.addClass(classOf[Contest])
		val r = new TypeRecombinator(RecombinationHints.create(Map()), db)
		val materialized = r.materialize[DecideProcess[List[Patch], Patch]]

		val processClasses = materialized.map(_.clazz).toSet
		Assert.assertEquals(Set(classOf[Contest]), processClasses)
	}

	@Test
	def testParameterSupplyingConstraints: Unit = {
		val possibleValue1 = 17
		val possibleValue2 = 23
		val workerCountHint = new AddedParameterRecombinationHint[Int](DefaultParameters.WORKER_COUNT, List(possibleValue1, possibleValue2))
		val settingsHint = new SettingsOnParamsRecombinationHint(addDefaultValuesForParam = Some(false))
		val db = newDB

		val r = new TypeRecombinator(RecombinationHints.create(Map(
			RecombinationHints.DEFAULT_HINTS -> List(workerCountHint, settingsHint)
		)), db)
		val materialized = r.materialize[ProcessStub[Patch, List[Patch]]]

		Assert.assertTrue(materialized.forall(p => {
			val thisParam = p.getParam(workerCountHint.param.key)
			val valueIsSet = thisParam == Some(possibleValue1) || thisParam == Some(possibleValue2)
			valueIsSet && p.params.size == 1
		}))
		Assert.assertEquals(4, materialized.length)
	}

	@Test
	def testCollectDecideProcessRecombinationWithSimpleDB: Unit = {
		val db = newDB
		db.addClass(classOf[Contest])
		db.addClass(classOf[ContestWithBeatByKVotingProcess])
		db.addClass(classOf[CollectDecideProcess])

		val parametersToDisableDefaultValues: Map[Class[_ <: ProcessStub[_, _]], List[RecombinationHint]] = Map(
			RecombinationHints.DEFAULT_HINTS -> List(new SettingsOnParamsRecombinationHint(addDefaultValuesForParam = Some(false))))
		val r = new TypeRecombinator(RecombinationHints.create(parametersToDisableDefaultValues), db)
		val materialized = r.materialize[CollectDecideProcess]

		Assert.assertEquals(4, materialized.size)
	}

	@Test
	def testTurningDefaultOffAndForceParamSetting: Unit = {
		val materialized = new TextShorteningRecombinationTest().candidates

		def containsSubProcessWithPortal(process: PassableProcessParam[_], targetPortal: HCompPortalAdapter): Boolean = {
			val OneOfChildrenContainsPortal = process.params.values.exists {
				case proc: PassableProcessParam[_] =>
					containsSubProcessWithPortal(proc, targetPortal)
				case _ => false
			}

			val thisProcessContainsPortal = process.getParam(DefaultParameters.PORTAL_PARAMETER) == Some(targetPortal)

			thisProcessContainsPortal && OneOfChildrenContainsPortal
		}
		val otherPortals = HComp.allDefinedPortals.toSet - HComp.randomPortal
		if (otherPortals.size == 0) println(Thread.currentThread().getStackTrace()(1) + ": This test will only work if you have defined more than 1 portal.")

		Assert.assertFalse("no crowd flower must be used, only mturk", materialized.exists(p =>
			otherPortals.exists(portal => containsSubProcessWithPortal(p, portal))))
	}
}

object TypeRecombinatorTest {
	val DEFAULT_TESTING_HINTS: Map[Class[_ <: ProcessStub[_, _]], List[RecombinationHint]] = Map(RecombinationHints.DEFAULT_HINTS -> (List(
		//disable default values for instruction values
		new SettingsOnParamsRecombinationHint(List(DefaultParameters.INSTRUCTIONS.key), addDefaultValuesForParam = Some(false)),
		new AddedParameterRecombinationHint[InstructionData](DefaultParameters.INSTRUCTIONS, List(
			new InstructionData(actionName = "shorten the following paragraph", detailedDescription = "grammar (e.g. tenses), text-length")))
	) ::: RecombinationHints.hcompPlatform(List(HComp.randomPortal))))
}