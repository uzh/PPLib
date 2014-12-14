package ch.uzh.ifi.pdeboer.pplib.examples

import ch.uzh.ifi.pdeboer.pplib.process.entities.StringPatch
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.NaiveSelectionProcess
import NaiveSelectionProcess._

/**
 * Created by pdeboer on 14/12/14.
 */
object FindAndFix extends App {
	val data = List("correct1", "correct2", "error1", "correct3").map(p => new StringPatch(p))
	val findProcess = new NaiveSelectionProcess(Map(FINDERS_PER_ITEM.key -> 2))
	val result = findProcess.process(data)
	println(result)
}
