package ch.uzh.ifi.pdeboer.crowdlang.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
trait RecombinatedSubject {
  def runRecombinedVariant(config: RecombinationVariant)

  def allRecombinationKeys: List[String]
}
