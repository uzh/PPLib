package ch.uzh.ifi.pdeboer.pplib.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
case class RecombinationVariant(stubs: Map[String, ProcessStub[_, _]]) {

	def apply[IN, OUT](key: String) = stubs(key).asInstanceOf[ProcessStub[IN, OUT]]

	def totalCost = stubs.values.map {
		case v: ProcessStubWithHCompPortalAccess[_, _] => v.portal.cost
		case _ => 0d
	}.sum
}
