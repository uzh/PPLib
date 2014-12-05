package ch.uzh.ifi.pdeboer.pplib.recombination.stdlib

import ch.uzh.ifi.pdeboer.pplib.recombination.ProcessStub

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 05/12/14.
 */
class IdleProcess[IN: ClassTag](params: Map[String, Any] = Map.empty) extends ProcessStub[IN, IN](params) {
	override protected def run(data: IN) = data
}
