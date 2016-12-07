package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompAnswer

import scala.collection.mutable

/**
  * Created by pdeboer on 14/03/16.
  */
class Vote(val selectedAnswer: Patch, val rawAnswer: HCompAnswer, var weight: Double = 1.0d, var parsedData: Map[String, Any] = Map.empty) {

	def canEqual(other: Any): Boolean = other.isInstanceOf[Vote]

	override def equals(other: Any): Boolean = {
		if (this canEqual other) {
			val that = other.asInstanceOf[Vote]
			selectedAnswer == that.selectedAnswer &&
				rawAnswer == that.rawAnswer &&
				Math.abs(weight - that.weight) < 0.05 &&
				parsedData == that.parsedData
		} else false
	}

	override def toString = s"Vote($selectedAnswer, $weight)"
}

class VotesTable extends Serializable {
	private var _answers = mutable.HashMap.empty[Patch, List[Vote]]

	def clear(): Unit = {
		_answers = mutable.HashMap.empty[Patch, List[Vote]]
	}

	protected def +=(v: Vote) = _answers += (v.selectedAnswer -> (v :: _answers.getOrElse(v.selectedAnswer, List.empty[Vote])))

	def answers = _answers.toMap

	def votesCount = _answers.values.flatten.map(_.weight).sum

	def votesCountFor(input: Patch) = _answers.getOrElse(input, Nil).map(_.weight).sum



	def addVote(selectedAnswer: Patch, rawAnswer: HCompAnswer) = {
		val v = new Vote(selectedAnswer, rawAnswer)
		this += v
		v
	}

	def sortedByWeight = answers.toList.sortBy(_._2.map(_.weight).sum * -1).map(k => (k._1, k._2.map(_.weight).sum))


	def canEqual(other: Any): Boolean = other.isInstanceOf[VotesTable]

	override def equals(other: Any): Boolean = other match {
		case that: VotesTable =>
			(that canEqual this) &&
				_answers.forall(ta => {
					that._answers.getOrElse(ta._1, Nil) zip ta._2 forall (tita => {
						tita._1.equals(tita._2)
					})
				}) &&
				_answers.size == that._answers.size
		case _ => false
	}
}

