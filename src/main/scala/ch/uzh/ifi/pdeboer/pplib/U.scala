package ch.uzh.ifi.pdeboer.pplib

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Created by pdeboer on 15/10/14.
 */
object U {
	/**
	 * Method used to retry some code that may fail n times.
	 * @param n  how often to retry
	 * @param fn  the fallible function
	 * @tparam T return value of the function
	 * @return the result of the function
	 */
	def retry[T](n: Int)(fn: => T): T = {
		try {
			fn
		} catch {
			case e if n > 1 =>
				retry(n - 1)(fn)
		}
	}

	def getConfigString(name: String): Option[String] = {
		val config: Config = ConfigFactory.load()
		try {
			Some(config.getString(name))
		}
		catch {
			case _ => None
		}
	}
}
