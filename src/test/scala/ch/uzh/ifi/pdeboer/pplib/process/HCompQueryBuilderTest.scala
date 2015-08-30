package ch.uzh.ifi.pdeboer.pplib.process

import ch.uzh.ifi.pdeboer.pplib.process.stdlib.ContestWithBeatByKVotingProcess
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 30/08/15.
 */
class HCompQueryBuilderTest {
	@Test
	def testProcessParamDefault: Unit = {
		val bbk = new ContestWithBeatByKVotingProcess()
		Assert.assertEquals(bbk.getParam(bbk.queryBuilderParam), bbk.processParameterDefaults(bbk.queryBuilderParam).head)
	}
}
