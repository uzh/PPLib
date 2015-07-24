package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.FixPatchProcess

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 05/12/14.
 */

import scala.reflect.runtime.universe._

trait ParamOverridenCostCeiling {
	self: ProcessStub[_, _] =>
	def defaultCostCeiling: Int = params.getOrElse("OverrideCostCeiling", "1").toString.toInt
}

class CreateSignalingProcess[IN, OUT](params: Map[String, Any] = Map.empty)(implicit inputClass: ClassTag[IN], outputClass: ClassTag[OUT], inputType1: TypeTag[IN], outputType1: TypeTag[OUT]) extends CreateProcess[IN, OUT](params) with ParamOverridenCostCeiling {
	var called: Boolean = false

	override protected def run(data: IN): OUT = {
		called = true
		CreateSignalingProcess.OUTPUT.get.asInstanceOf[OUT]
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(CreateSignalingProcess.OUTPUT)

	override def getCostCeiling(data: IN): Int = defaultCostCeiling
}

object CreateSignalingProcess {
	val OUTPUT = new ProcessParameter[AnyRef]("out", None)
}

class CreateSignalingProcessFactory[IN, OUT]()(implicit inputClass: ClassTag[IN], outputClass: ClassTag[OUT], inputType1: TypeTag[IN], outputType1: TypeTag[OUT]) extends ProcessFactory[CreateSignalingProcess[IN, OUT]] {
	override def buildProcess(params: Map[String, Any]) =
		new CreateSignalingProcess[IN, OUT](params)

	override def typelessBuildProcess(params: Map[String, Any]): ProcessStub[_, _] = {
		throw new IllegalArgumentException("cant touch this")
	}
}

class DecideSignalingProcess[IN, OUT](params: Map[String, Any] = Map.empty)(implicit inputClass: ClassTag[IN], outputClass: ClassTag[OUT], inputType1: TypeTag[IN], outputType1: TypeTag[OUT]) extends DecideProcess[IN, OUT](params) with ParamOverridenCostCeiling {
	var called: Boolean = false

	override protected def run(data: IN): OUT = {
		called = true
		DecideSignalingProcess.OUTPUT.get.asInstanceOf[OUT]
	}

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(DecideSignalingProcess.OUTPUT)

	override def getCostCeiling(data: IN): Int = defaultCostCeiling

}

object DecideSignalingProcess {
	val OUTPUT = new ProcessParameter[AnyRef]("out", None)
}

class DecideSignalingProcessFactory[IN, OUT]()(implicit inputClass: ClassTag[IN], outputClass: ClassTag[OUT], inputType1: TypeTag[IN], outputType1: TypeTag[OUT]) extends ProcessFactory[DecideSignalingProcess[IN, OUT]] {
	override def buildProcess(params: Map[String, Any]) =
		new DecideSignalingProcess[IN, OUT](params)

	override def typelessBuildProcess(params: Map[String, Any]): ProcessStub[_, _] = {
		throw new IllegalArgumentException("cant touch this")
	}
}

class FixSignalingProcess(params: Map[String, Any] = Map.empty) extends FixPatchProcess(params) {
	var called: Boolean = false


	override protected def run(data: List[Patch]): List[Patch] = {
		called = true
		FixSignalingProcess.OUTPUT.get
	}


	override def expectedParametersBeforeRun: List[ProcessParameter[_]] = List(FixSignalingProcess.OUTPUT)
}

object FixSignalingProcess {
	val OUTPUT = new ProcessParameter[List[IndexedPatch]]("out", None)
}

class FixSignalingProcessFactory() extends ProcessFactory[FixSignalingProcess] {
	override def buildProcess(params: Map[String, Any]) =
		new FixSignalingProcess(params)

	override def typelessBuildProcess(params: Map[String, Any]): ProcessStub[_, _] = {
		throw new IllegalArgumentException("cant touch this")
	}
}