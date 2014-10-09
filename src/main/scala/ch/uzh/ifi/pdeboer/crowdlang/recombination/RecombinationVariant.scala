package ch.uzh.ifi.pdeboer.crowdlang.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
class RecombinationVariant(config: Map[String, RecombinationStub]) {
  def apply(key: String) = config(key)
}
