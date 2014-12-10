package ch.uzh.ifi.pdeboer.pplib.process.entities

/**
 * Created by pdeboer on 05/12/14.
 */
class Patch(val payload: Serializable) extends Serializable {
	override def toString = payload.toString

	override def equals(other: Any): Boolean = other match {
		case that: Patch =>
				payload == that.payload
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(payload)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}

case class StringWrapper(str: String) extends Serializable {
	override def toString: String = str
}

class StringPatch(_payload: String) extends Patch(StringWrapper(_payload))
