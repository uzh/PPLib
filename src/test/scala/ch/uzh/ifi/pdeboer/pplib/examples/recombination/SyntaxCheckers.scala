package ch.uzh.ifi.pdeboer.pplib.examples.recombination

import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.FindFixVerifyProcess
import ch.uzh.ifi.pdeboer.pplib.recombination.{ProcessParamter, ProcessStub}

/**
 * Created by pdeboer on 04/11/14.
 */
class FFVSyntaxChecker(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, String](params) {

	import ch.uzh.ifi.pdeboer.pplib.examples.recombination.FFVSyntaxChecker._

	lazy val ffvProcess = new FindFixVerifyProcess(params)

	override protected def run(data: String): String = {
		val input = data.split(getParamUnsafe(SPLIT_EXPLODER)).toList
		ffvProcess.process(input).mkString(getParamUnsafe(SPLIT_IMPLODER))
	}

	override def expectedParametersBeforeRun: List[ProcessParamter[_]] =
		ffvProcess.expectedParametersBeforeRun

	override def optionalParameters: List[ProcessParamter[_]] =
		ffvProcess.optionalParameters ::: List(SPLIT_EXPLODER, SPLIT_IMPLODER)

}

object FFVSyntaxChecker {
	val SPLIT_EXPLODER = new ProcessParamter[String]("splitExploder", Some(List("\\.")))
	val SPLIT_IMPLODER = new ProcessParamter[String]("splitImploder", Some(List(".")))

}
