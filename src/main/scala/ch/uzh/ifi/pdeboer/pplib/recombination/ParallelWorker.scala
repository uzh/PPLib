package ch.uzh.ifi.pdeboer.pplib.recombination

import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.collection.parallel.{SeqSplitter, ForkJoinTaskSupport, ParSeq}
import scala.concurrent.forkjoin.ForkJoinPool

/**
 * Created by pdeboer on 26/11/14.
 */
class ParallelCollectionWithWorkerConnection[N](val decorated: ParSeq[N]) extends ParSeq[N] {
	def assignPool(pool: ForkJoinPool): ParSeq[N] = {
		this.tasksupport = new ForkJoinTaskSupport(pool)
		this
	}

	def assignWorkerPool(): ParSeq[N] = assignPool(U.hugeForkJoinPool)

	override def apply(i: Int): N = decorated.apply(i)

	//TODO this is bad
	override def splitter: SeqSplitter[N] = ???

	override def length: Int = decorated.length

	override def seq: Seq[N] = decorated.seq
}

object ParSeqToAssignPool {
	implicit def parSeqToWorkerConnection[T](seq: ParSeq[T]): ParallelCollectionWithWorkerConnection[T] = new ParallelCollectionWithWorkerConnection[T](seq)
}