package ch.uzh.ifi.pdeboer.crowdlang.hcomp

import ch.uzh.ifi.pdeboer.crowdlang.hcomp.crowdflower.CrowdFlowerPortalAdapter

/**
 * Created by pdeboer on 10/10/14.
 */
object HComp {
	var crowdFlower: CrowdFlowerPortalAdapter = null
	var mechanicalTurk: HCompPortalAdapter = null

	def allDefinedPortals(): List[HCompPortalAdapter] = List(crowdFlower, mechanicalTurk).filter(_ != null)
}
