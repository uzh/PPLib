package ch.uzh.ifi.pdeboer.pplib.util

/**
 * Created by pdeboer on 12/05/15.
 */
case class StringWrapper(str: String) extends Serializable {
	override def toString: String = str
}
