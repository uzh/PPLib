package ch.uzh.ifi.pdeboer.pplib.examples

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompInstructionsWithTuple, HComp}
import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.patterns.{FFVPatch, FFVDefaultHCompDriver, FindFixVerifyExecutor}

/**
 * Created by pdeboer on 30/10/14.
 */
object FindFixVerifyTranslation extends App {
	//source: google translate of first paragraph from http://www.heise.de/newsticker/meldung/Google-will-Nanopartikel-nach-Krebszellen-suchen-lassen-2438107.html
	val textToImprove =
		"""Google developed nanoparticles that are to detect signs of cancer or other disease
		  | in the bloodstream of a human being. The particles should be able to dock with cells,
		  | proteins or other molecules within a human body, explained Andrew Conrad, head of the
		  | team for life sciences laboratory Google X at the conference WSJD Live the
		  | Wall Street Journal. Simultaneously, a bracelet was being developed that can attract
		  | and include the magnetic nanoparticles. This should be part of a constantly running
		  | early warning system for life-threatening diseases.""".stripMargin

	val patches = textToImprove.split("\\.").map(_.trim).toList

	HComp.addPortal(new CrowdFlowerPortalAdapter("Fix-Up English Translation", sandbox = true))

	val ffv = new FindFixVerifyExecutor[String](
		new FFVDefaultHCompDriver(
			patches.zipWithIndex.map(p => FFVPatch[String](p._1, p._2)),
			HComp("crowdFlower")
		), findersCount = 1, minFindersCountThatNeedToAgreeForFix = 1, fixersPerPatch = 1)

	println("ended :" + ffv.bestPatches.map(_.patch).mkString("."))
}
