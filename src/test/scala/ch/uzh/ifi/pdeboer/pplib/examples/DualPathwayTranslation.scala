package ch.uzh.ifi.pdeboer.pplib.examples

import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CrowdFlowerPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompInstructionsWithData}
import ch.uzh.ifi.pdeboer.pplib.patterns.DPHCompDriverDefaultComparisonInstructionsConfig
import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.DualPathwayProcess

/**
 * Created by pdeboer on 12/11/14.
 */
object DualPathwayTranslation extends App {

	import DualPathwayProcess._

	private val textToImprove: String =
		"""China has for the first time sent a probe to the moon and back to Earth. The return capsule landed after a circumlunar flight of the orbiter on Saturday morning in Mongolia. Eight days took the 840,000 km long journey. It was the world's first mission of its kind since almost 40 years, state media reported.
		  |After the US and the former Soviet Union, China is therefore the third country to which such a successful project. The test was in preparation for China's first lunar landing with subsequent return. In this planned for 2017 Flight China will not only put a probe on the Earth's satellite, but this then get back together with soil samples again to the earth.
		  |China is pushing its space program with great strides. The first space probe 'Chang'e 3 "had landed on 15 December 2013, the Moon and the vehicle had" Jadehase "exposed (Yutu). This was China after the United States and the Soviet Union the third nation in the world that has made a moon landing.
		  |The moon flights to demonstrate the technological capability of China's second largest economy""".stripMargin

	val paragraphs = textToImprove.split("\n").map(_.trim).toList

	HComp.addPortal(new CrowdFlowerPortalAdapter("Fix-Up English Translation", sandbox = true))

	val process = new DualPathwayProcess(Map(
		QUESTION_PER_COMPARISON_TASK.key -> new DPHCompDriverDefaultComparisonInstructionsConfig(
			"Comparison",
			"Please compare the two columns below established by other crowd workers. Are they roughly equal? "),
		QUESTION_PER_PROCESSING_TASK.key -> "Check the items below and make sure they are fixed correctly.",
		QUESTION_OLD_PROCESSED_ELEMENT.key -> HCompInstructionsWithData("Please ensure that the following element is spelled out correctly"),
		QUESTION_NEW_PROCESSED_ELEMENT.key -> HCompInstructionsWithData("Please fix grammatical mistakes and spelling errors in the following paragraph by rewriting it into the text box below.")
	))

	val res = process.process(paragraphs)
}
