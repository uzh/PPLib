package ch.uzh.ifi.pdeboer.crowdlang.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
case class RecombinationVariant(config: Map[String, RecombinationStub[_, _, _, _]]) {
  def apply(key: String) = config(key)
}
