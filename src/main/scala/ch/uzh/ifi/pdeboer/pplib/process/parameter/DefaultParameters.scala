package ch.uzh.ifi.pdeboer.pplib.process.parameter

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompQueryProperties, HComp, HCompPortalAdapter}

import scala.xml.NodeSeq

/**
 * Created by pdeboer on 09/02/15.
 */
object DefaultParameters {
	val PORTAL_PARAMETER = new ProcessParameter[HCompPortalAdapter]("portal", Some(HComp.allDefinedPortals))
	val PARALLEL_EXECUTION_PARAMETER = new ProcessParameter[Boolean]("parallel", Some(List(true)))

	val STORE_EXECUTION_RESULTS = new ProcessParameter[Boolean]("storeExecutionResults", Some(List(true)))
	val MEMOIZER_NAME = new ProcessParameter[Option[String]]("memoizerName", Some(List(None)))

	val INSTRUCTIONS = new ProcessParameter[InstructionData]("instructions", Some(List(new InstructionData())))
	val INSTRUCTIONS_ITALIC = new ProcessParameter[String]("instructionItalic", Some(List("")))
	val QUESTION_AUX = new ProcessParameter[Option[NodeSeq]]("questionAux", Some(List(None)))
	val WORKER_COUNT = new ProcessParameter[Int]("workerCount", Some(List(5)))
	val QUESTION_PRICE = new ProcessParameter[HCompQueryProperties]("cost", Some(List(HCompQueryProperties())))
	val OVERRIDE_INSTRUCTION_GENERATOR = new ProcessParameter[InstructionGenerator]("instructionGenerator", None)

	val SHUFFLE_CHOICES = new ProcessParameter[Boolean]("shuffle", Some(List(true)))

	val MAX_ITERATIONS = new ProcessParameter[Int]("maxIterations", Some(List(20)))

}
