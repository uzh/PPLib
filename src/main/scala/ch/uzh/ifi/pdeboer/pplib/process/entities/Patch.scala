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

class PatchWithIndex(_payload: Serializable, val index: Int) extends Patch(_payload)

case class StringWrapper(str: String) extends Serializable {
	override def toString: String = str
}

class StringPatch(_payload: String) extends Patch(StringWrapper(_payload))

class StringPatchWithIndex(_payload: String, _index: Int) extends PatchWithIndex(StringWrapper(_payload), _index) {
	def this(p: (String, Int)) = this(p._1, p._2)
}