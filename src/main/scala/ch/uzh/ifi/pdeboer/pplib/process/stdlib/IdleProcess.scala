package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
/**
 * Created by pdeboer on 05/12/14.
 */
class IdleProcess[IN](params: Map[String, Any] = Map.empty)(implicit ttag: TypeTag[IN], clsTag: ClassTag[IN]) extends ProcessStub[IN, IN](params) {
	override protected def run(data: IN) = data
}
