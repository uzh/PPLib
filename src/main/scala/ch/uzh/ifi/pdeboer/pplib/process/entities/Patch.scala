package ch.uzh.ifi.pdeboer.pplib.process.entities

/**
 * Created by pdeboer on 05/12/14.
 */
class Patch(val payload: Any) {
	override def toString = payload.toString

	def canEqual(other: Any): Boolean = other.isInstanceOf[Patch]

	override def equals(other: Any): Boolean = other match {
		case that: Patch =>
			(that canEqual this) &&
				payload == that.payload
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(payload)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}

class StringPatch(stringValue: String, payload: Any) extends Patch(payload) {
	override def toString = stringValue
}
