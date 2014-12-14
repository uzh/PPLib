package ch.uzh.ifi.pdeboer.pplib.process

/**
 * Created by pdeboer on 14/12/14.
 */
class VerifyTestProcessStub(params: Map[String, Any] = Map.empty) extends ProcessStub[List[String], String](params) {
	var wasCalled: Boolean = false

	override protected def run(data: List[String]): String = {
		wasCalled = true
		"a"
	}
}
