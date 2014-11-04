package ch.uzh.ifi.pdeboer.pplib.examples.translator

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompPortalAdapter}
import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.{DualPathwayProcess, FindFixVerifyProcess}
import ch.uzh.ifi.pdeboer.pplib.recombination.{RecombinationParameter, RecombinationStub}

/**
 * Created by pdeboer on 04/11/14.
 */
class DPParagraphRewrite(params: Map[String, Any] = Map.empty[String, Any]) extends DualPathwayProcess(params) {
	override protected def run(data: List[String]): List[String] = {
		//the List[String] we get is a list of paragraphs.
		// We want to execute dual-pathway on each sentence of each paragraph.
		data.map(r => super.run(r.split("\\.").toList).mkString("."))
	}
}

class FFVParagraphRewrite(params: Map[String, Any] = Map.empty[String, Any]) extends FindFixVerifyProcess(params) {
	override protected def run(data: List[String]): List[String] = {
		data.map(r => super.run(r.split("\\.").toList).mkString("."))
	}
}

//TODO finish me
class NaiveRewriteSelectBest(params: Map[String, Any]) extends RecombinationStub[List[String], List[String]](params) {

	import ch.uzh.ifi.pdeboer.pplib.examples.translator.NaiveRewriteSelectBest._

	override protected def run(data: List[String]): List[String] = {
		data.map(r => proc(r.split("\\.").toList).mkString("."))
	}

	def proc(data: List[String]): List[String] = {
		val portal = getParamUnsafe(PORTAL_PARAMETER)

		//portal.sendQueryAndAwaitResult()
		???
	}

	override def expectedParametersBeforeRun: List[RecombinationParameter[_]] =
		List(PORTAL_PARAMETER)

	override def optionalParameters: List[RecombinationParameter[_]] =
		List(CROWD_WORKER_COUNT, QUESTION)
}

object NaiveRewriteSelectBest {
	val PORTAL_PARAMETER = new RecombinationParameter[HCompPortalAdapter]("portal", Some(HComp.allDefinedPortals))
	val CROWD_WORKER_COUNT = new RecombinationParameter[Integer]("workerCount", Some(List(3)))
	val QUESTION = new RecombinationParameter[String]("question", Some(List("Please rewrite this sentence to improve it.")))
}