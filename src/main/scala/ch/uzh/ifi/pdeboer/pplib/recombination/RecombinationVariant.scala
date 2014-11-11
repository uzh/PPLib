package ch.uzh.ifi.pdeboer.pplib.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
case class RecombinationVariant(stubs: Map[String, RecombinationStub[_, _]]) {

	def apply[IN, OUT](key: String) = stubs(key).asInstanceOf[RecombinationStub[IN, OUT]]

	def totalCost = stubs.values.map {
		case v: RecombinationStubWithHCompPortalAccess[_, _] => v.portal.cost
		case _ => 0d
	}.sum
}
