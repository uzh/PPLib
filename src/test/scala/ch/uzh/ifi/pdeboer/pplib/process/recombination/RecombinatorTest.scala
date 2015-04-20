package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{DecideProcess, DefaultParameters, Patch}
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
		val r = new Recombinator[Patch, List[Patch]](db = db)
		val materialized = r.materialize()

		val processClasses = materialized.map(_.clazz).toSet
		Assert.assertEquals(Set(classOf[Collection], classOf[CollectionWithSigmaPruning]), processClasses)
	}

	@Test
	def testTypeConstrainedMaterialize: Unit = {
		val db = newDB
		db.addClass(classOf[Contest])
		val tc = new TypeRecombinationHint[DecideProcess[List[Patch], Patch]]()

		val r = new Recombinator[List[Patch], Patch](List(new RecombinationHintGroup(None, List(tc))), db)
		val materialized = r.materialize()

		val processClasses = materialized.map(_.clazz).toSet
		Assert.assertEquals(Set(classOf[Contest]), processClasses)
	}


	@Test
	def testParameterSupplyingConstraints: Unit = {
		val possibleValue1: String = "test1"
		val possibleValue2: String = "test2"
		val tc = new OptionalParameterRecombinationHint[String](DefaultParameters.INSTRUCTIONS_ITALIC, List(possibleValue1, possibleValue2))
		val db = newDB

		val r = new Recombinator[Patch, List[Patch]](List(new RecombinationHintGroup(None, List(tc))), db)
		val materialized = r.materialize()

		Assert.assertTrue(materialized.forall(p => {
			val thisParam = p.getParam(tc.param.key)
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
		val tc = new TypeRecombinationHint[CollectDecideProcess]()

		val r = new Recombinator[Patch, Patch](List(new RecombinationHintGroup(None, List(tc))), db)
		val materialized = r.materialize()

		val processClasses = materialized.map(_.clazz).toSet
		Assert.assertEquals(4, processClasses.size)
	}
}
