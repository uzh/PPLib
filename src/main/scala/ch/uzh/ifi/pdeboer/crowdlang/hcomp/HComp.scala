package ch.uzh.ifi.pdeboer.crowdlang.hcomp

/**
 * Created by pdeboer on 10/10/14.
 */
object HComp {
	var crowdFlower: HCompPortalAdapter = null
	var mechanicalTurk: HCompPortalAdapter = null

	def allDefinedPortals() = List(crowdFlower, mechanicalTurk).filter(_ != null)
}
