package ch.uzh.ifi.pdeboer.pplib.process.parameter

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompPortalAdapter}

/**
 * Created by pdeboer on 09/02/15.
 */
object DefaultParameters {
	val PORTAL_PARAMETER = new ProcessParameter[HCompPortalAdapter]("portal", Some(HComp.allDefinedPortals))
	val PARALLEL_EXECUTION_PARAMETER = new ProcessParameter[Boolean]("parallel", Some(List(true)))

	val STORE_EXECUTION_RESULTS = new ProcessParameter[Boolean]("storeExecutionResults", Some(List(true)))
	val MEMOIZER_NAME = new ProcessParameter[Option[String]]("memoizerName", Some(List(None)))

	val INSTRUCTIONS = new ProcessParameter[InstructionData]("", None)
}
