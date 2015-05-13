package ch.uzh.ifi.pdeboer.pplib.examples.textshortening

import ch.uzh.ifi.pdeboer.pplib.hcomp.randomportal.RandomHCompPortalWithDefinedCandidates

/**
 * Created by pdeboer on 13/05/15.
 */
class ShortNTestDataInitializer {
	private val line1: String = "This text is way too long and could be shortened by anyone except for people who can't."
	private val line2: String = "The 2nd sentence is also very very useless."
	private val line3: String = "And the third one as well - very much so."

	val text: String = List(line1, line2, line3).mkString("\n")

	val hcompPortalWithMockAnswers =
		new RandomHCompPortalWithDefinedCandidates(Map(line1 -> List(
			"This text is too long and could be shortened by anyone except for people who can't.",
			"This text is long and could be shortened by anyone except for people who can't.",
			"This text is way too long and could be shortened",
			"This text is way too long and could be shortened by anyone",
			"This text is long and could be shortened",
			"This text is too long"
		),
			line2 -> List(
				"The 2nd sentence is also very useless.",
				"The 2nd sentence is also really useless.",
				"The 2nd sentence is also useless.",
				"The 2nd sentence is useless."
			),
			line3 -> List(
				"And the third one as well - very much so.",
				"And the third one as well",
				"And the third one as well - indeed.",
				"The third one too - very much so.",
				"The third one too - indeed."
			)))
}
