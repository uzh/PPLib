package ch.uzh.ifi.pdeboer.pplib.process

import ch.uzh.ifi.pdeboer.pplib.process.parameter.Patch

/**
 * Created by pdeboer on 14/12/14.
 */
class VerifyTestProcessStub(params: Map[String, Any] = Map.empty) extends ProcessStub[List[Patch], Patch](params) {
	var wasCalled: Boolean = false

	override protected def run(data: List[Patch]): Patch = {
		wasCalled = true
		new Patch("a")
	}
}
