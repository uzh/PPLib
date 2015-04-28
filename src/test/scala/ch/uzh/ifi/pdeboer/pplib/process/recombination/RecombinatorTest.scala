package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib._
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 27/03/15.
 */
class RecombinatorTest {
	def newDB = {
		val db = new RecombinationDB
		db.addClass(classOf[Collection])
		db.addClass(classOf[CollectionWithSigmaPruning])
		db
	}

	@Test
	def testTrivialMaterialize: Unit = {
		val db = newDB
		val r = new Recombinator(RecombinationHints.create(Map()), db)
		val materialized = r.materialize[CreateProcess[Patch, List[Patch]]]

		val processClasses = materialized.map(_.clazz).toSet
		Assert.assertEquals(Set(classOf[Collection], classOf[CollectionWithSigmaPruning]), processClasses)
	}

	@Test
	def testTypeConstrainedMaterialize: Unit = {
		val db = newDB
		db.addClass(classOf[Contest])
		val r = new Recombinator(RecombinationHints.create(Map()), db)
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

		val r = new Recombinator(RecombinationHints.create(Map(
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
		val r = new Recombinator(RecombinationHints.create(parametersToDisableDefaultValues), db)
		val materialized = r.materialize[CollectDecideProcess]

		Assert.assertEquals(4, materialized.size)
	}
}
