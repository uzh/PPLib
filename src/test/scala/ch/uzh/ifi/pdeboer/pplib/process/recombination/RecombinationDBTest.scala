package ch.uzh.ifi.pdeboer.pplib.process.recombination

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 27/03/15.
 */
class RecombinationDBTest {
	@Test
	def testAutoInit(): Unit = {
		val myDB = new RecombinationDB
		new PPLibAnnotationLoader(myDB).load()

		Assert.assertTrue(myDB.classes.size > 5)
	}
}
