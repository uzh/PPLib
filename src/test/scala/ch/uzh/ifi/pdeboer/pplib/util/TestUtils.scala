package ch.uzh.ifi.pdeboer.pplib.util

import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal.RandomHCompPortal

/**
 * Created by pdeboer on 07/07/15.
 */
object TestUtils {
	def ensureThereIsAtLeast1Portal(): Unit = {
		if (HComp.allDefinedPortals.isEmpty) {
			Thread.sleep(1000)
			HComp.addPortal(new RandomHCompPortal(""))
		}
	}
}
