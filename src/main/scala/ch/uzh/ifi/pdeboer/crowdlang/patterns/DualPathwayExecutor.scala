package ch.uzh.ifi.pdeboer.crowdlang.patterns

/**
 * Created by pdeboer on 13/10/14.
 */
//TODO code me
class DualPathwayExecutor {

}

trait DualPathwayConfiguration {
	//def addTask(task:TASK[_])
}

trait Task[TASK_OUTPUT] {
	def run(): TASK_OUTPUT
}