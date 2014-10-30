package ch.uzh.ifi.pdeboer.pplib.examples

import com.typesafe.config.ConfigFactory

/**
 * Created by pdeboer on 30/10/14.
 */
object FindFixVerifyTranslation extends App {
	val config = ConfigFactory.load()
	config.getString("hcomp.crowdflower.apikey")
}
