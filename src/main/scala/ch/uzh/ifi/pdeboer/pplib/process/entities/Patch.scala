package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.util.StringWrapper

/**
 * Created by pdeboer on 05/12/14.
 */
@SerialVersionUID(1l)
class Patch(val value: String, val payload: Option[_ <: Serializable] = None) extends Serializable {
	var auxiliaryInformation: Map[String, Any] = Map.empty

	def canEqual(other: Any): Boolean = other.isInstanceOf[Patch]

	override def equals(other: Any): Boolean = other match {
		case that: Patch =>
			(that canEqual this) &&
				value == that.value
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(value)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}

	override def toString = value

	def duplicate(value: String, payload: Option[_ <: Serializable] = this.payload) = {
		val p = new Patch(value, payload)
		p.auxiliaryInformation = auxiliaryInformation
		p
	}
}

@SerialVersionUID(1l)
class IndexedPatch(value: String, val index: Int, payload: Option[_ <: Serializable] = None) extends Patch(value, payload) with Serializable {
	def this(t: (String, Int)) = this(t._1, t._2)

	override def duplicate(value: String, payload: Option[_ <: Serializable] = this.payload): Patch = {
		val p = new IndexedPatch(value, index, payload)
		p.auxiliaryInformation = auxiliaryInformation
		p
	}


	override def canEqual(other: Any): Boolean = other.isInstanceOf[IndexedPatch]

	override def equals(other: Any): Boolean = other match {
		case that: IndexedPatch =>
			super.equals(that) &&
				(that canEqual this) &&
				index == that.index
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(super.hashCode(), index)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}

	override def toString = value
}

object IndexedPatch {
	def from(str: String, delimiter: String = "\n"): List[IndexedPatch] = {
		IndexedPatch.from(str.split(delimiter).toList)
	}

	def from(l: List[String]): List[IndexedPatch] = l.zipWithIndex.map(s => new IndexedPatch(s._1, s._2, Some(StringWrapper(s._1))))
}

object PatchConversion {
	implicit def patchToString(p: Patch): String = p.toString
}