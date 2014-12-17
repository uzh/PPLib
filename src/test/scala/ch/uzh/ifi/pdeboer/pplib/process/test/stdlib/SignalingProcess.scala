package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.{OtherParam, ProcessFactory, ProcessParameter, ProcessStub}

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 05/12/14.
 */

class SignalingProcess[IN: ClassTag, OUT: ClassTag](params: Map[String, Any] = Map.empty) extends ProcessStub[IN, OUT](params) {
	var called: Boolean = false

	override protected def run(data: IN): OUT = {
		called = true
		SignalingProcess.OUTPUT.get.asInstanceOf[OUT]
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(SignalingProcess.OUTPUT)
}

object SignalingProcess {
	val OUTPUT = new ProcessParameter[AnyRef]("out", OtherParam(), None)
}

class SignalingProcessFactory extends ProcessFactory {
	override def buildProcess[IN: ClassTag, OUT: ClassTag](params: Map[String, Any]): ProcessStub[IN, OUT] =
		new SignalingProcess[IN, OUT](params)

	override def typelessBuildProcess(params: Map[String, Any]): ProcessStub[_, _] = {
		throw new IllegalArgumentException("cant touch this")
	}
}