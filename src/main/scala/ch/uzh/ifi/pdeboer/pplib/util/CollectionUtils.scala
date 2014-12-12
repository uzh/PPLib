package ch.uzh.ifi.pdeboer.pplib.util

import scala.collection.parallel.{ForkJoinTaskSupport, ParSeq}

object CollectionUtils {
	implicit def seqToMPar[T](seq: Seq[T]): MParConverter[T] = new MParConverter[T](seq)
}

class MParConverter[+T](val seq: Seq[T]) {
	def mpar: ParSeq[T] = {
		val parSeq: ParSeq[T] = seq.par
		parSeq.tasksupport = new ForkJoinTaskSupport(U.hugeForkJoinPool)
		parSeq
	}
}
