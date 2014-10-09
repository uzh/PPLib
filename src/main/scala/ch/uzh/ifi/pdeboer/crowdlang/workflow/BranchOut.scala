package ch.uzh.ifi.pdeboer.crowdlang.workflow

import scala.collection.immutable.List

/**
 * Created by pdeboer on 09/10/14.
 */
class BranchOut[INPUT, SPLIT_OUTPUT, PROCESS_OUTPUT, OUTPUT](splitOperation: INPUT => List[SPLIT_OUTPUT],
                                                             processingOperation: SPLIT_OUTPUT => PROCESS_OUTPUT,
                                                             aggregationOperation: List[PROCESS_OUTPUT] => OUTPUT,
                                                             preserveOrder: Boolean = false) {


  def apply(data: INPUT): OUTPUT = {
    val splitted = splitOperation(data)
    val splittedSeq = if (preserveOrder) splitted else splitted.par

    val processed = splittedSeq.map(processingOperation).toList

    aggregationOperation(processed)
  }
}
