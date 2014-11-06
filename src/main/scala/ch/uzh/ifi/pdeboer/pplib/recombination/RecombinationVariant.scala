package ch.uzh.ifi.pdeboer.pplib.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
case class RecombinationVariant(config: Map[String, RecombinationStub[_, _]]) {

	def apply[IN, OUT](key: String) = config(key).asInstanceOf[RecombinationStub[IN, OUT]]

	def totalCost = config.values.map {
		case v: HCompPortalAccess[_, _] => v.portal.cost
		case _ => 0d
	}.sum
}
