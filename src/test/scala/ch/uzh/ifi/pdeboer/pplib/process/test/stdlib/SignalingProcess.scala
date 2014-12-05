package ch.uzh.ifi.pdeboer.pplib.process.test.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.ProcessStub

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 05/12/14.
 */

class SignalingProcess[IN: ClassTag, OUT: ClassTag](out: OUT) extends ProcessStub[IN, OUT]() {
	var called: Boolean = false

	override protected def run(data: IN): OUT = {
		called = true
		out
	}
}