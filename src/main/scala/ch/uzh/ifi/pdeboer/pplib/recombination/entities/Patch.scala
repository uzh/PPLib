package ch.uzh.ifi.pdeboer.pplib.recombination.entities

/**
 * Created by pdeboer on 05/12/14.
 */
class Patch[T](val payload: T) {
	override def toString = payload.toString

	def canEqual(other: Any): Boolean = other.isInstanceOf[Patch[_]]

	override def equals(other: Any): Boolean = other match {
		case that: Patch[_] =>
			(that canEqual this) &&
				payload == that.payload
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(payload)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}

class StringPatch[T](stringValue: String, payload: T) extends Patch[T](payload) {
	override def toString = stringValue
}
