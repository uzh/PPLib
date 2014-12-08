package ch.uzh.ifi.pdeboer.pplib.process

/**
 * Created by pdeboer on 05/12/14.
 */
trait ProcessMemoizer {
	def storeProcessState(implicit processStub: ProcessStub[_, _])
}
