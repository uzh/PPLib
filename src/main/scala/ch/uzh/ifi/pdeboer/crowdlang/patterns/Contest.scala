package ch.uzh.ifi.pdeboer.crowdlang.patterns

import scala.util.Random

/**
 * Created by pdeboer on 24/10/14.
 */
class ContestExecutor[T](val driver: ContestDriver[T], val showsPerElements: Int = 3, val maxElementsPerGo: Int = 100) {
	private val alternatives = driver.getAlternatives().zipWithIndex.map(a => a._2 -> new AlternativeDetails(a._1)).toMap

	def step: Unit = {
		val target = alternatives.values.view.filter(_.numberOfShows < showsPerElements).map((_, new Random().nextDouble())).toList.sortBy(_._2).take(maxElementsPerGo)
		target.foreach(e => {
			e._1.numberOfShows += 1
		})
		val selected = driver.getSingleVote(target.map(_._1.alternative))
		val selectedAlt = target.find(_._1.alternative == selected).get //crash if not found is ok
	}

	private case class AlternativeDetails(alternative: T, numberOfShows: Int = 0, numberOfSelects: Int = 0)

}

trait ContestDriver[T] {
	def getAlternatives(): List[T]

	def getSingleVote(options: List[T]): T
}
