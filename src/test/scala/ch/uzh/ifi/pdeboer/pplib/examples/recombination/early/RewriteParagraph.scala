package ch.uzh.ifi.pdeboer.pplib.examples.recombination

import ch.uzh.ifi.pdeboer.pplib.process.stdlib.{DualPathwayProcess, FindFixVerifyProcess}

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