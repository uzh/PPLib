package ch.uzh.ifi.pdeboer.pplib.examples.recombination

import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FindFixVerifyProcess
import ch.uzh.ifi.pdeboer.pplib.process.{OtherParam, ProcessParameter, ProcessStub}

/**
 * Created by pdeboer on 04/11/14.
 */
class FFVSyntaxChecker(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, String](params) {

	import ch.uzh.ifi.pdeboer.pplib.examples.recombination.FFVSyntaxChecker._

	lazy val ffvProcess = new FindFixVerifyProcess(params)

	override protected def run(data: String): String = {
		val input = data.split(getParam(SPLIT_EXPLODER)).toList
		ffvProcess.process(input).mkString(getParam(SPLIT_IMPLODER))
	}

	override def optionalParameters: List[ProcessParameter[_]] =
		ffvProcess.optionalParameters ::: List(SPLIT_EXPLODER, SPLIT_IMPLODER)

}

object FFVSyntaxChecker {
	val SPLIT_EXPLODER = new ProcessParameter[String]("splitExploder", OtherParam(), Some(List("\\.")))
	val SPLIT_IMPLODER = new ProcessParameter[String]("splitImploder", OtherParam(), Some(List(".")))

}
