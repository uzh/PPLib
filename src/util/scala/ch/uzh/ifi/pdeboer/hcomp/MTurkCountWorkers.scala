package ch.uzh.ifi.pdeboer.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp

/**
 * Created by pdeboer on 27/07/15.
 */
object MTurkCountWorkers extends App {

	case class HitResult(assId: String, turker: String)

	val service = HComp.mechanicalTurk.service
	val results = service.SearchHITs().par.flatMap(h => {
		println(s"working on hit $h")
		val assignments = try {
			service.GetAssignmentsForHIT(h.HITId)
		}
		catch {
			case e: Throwable => Nil
		}
		assignments.map(assignment => {
			HitResult(assignment.AssignmentId, assignment.WorkerId)
		})
	})

	println("number of hits: " + results.size)
	println("number of unique turkers: " + results.map(_.turker).toSet.size)
}
