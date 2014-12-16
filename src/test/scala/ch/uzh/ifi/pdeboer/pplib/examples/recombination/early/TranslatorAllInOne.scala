package ch.uzh.ifi.pdeboer.pplib.examples.recombination

import ch.uzh.ifi.pdeboer.pplib.process._
import ch.uzh.ifi.pdeboer.pplib.process.recombination._
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.{ContestWithStatisticalReductionProcess, FindFixVerifyProcess}

/**
 * Created by pdeboer on 04/11/14.
 */
object TranslatorAllInOneOLD extends App {
	//src: google translate of http://www.heise.de/newsticker/meldung/China-testet-erfolgreich-zweite-Mondsonde-2440834.html
	private val textToImprove: String =
		"""China has for the first time sent a probe to the moon and back to Earth. The return capsule landed after a circumlunar flight of the orbiter on Saturday morning in Mongolia. Eight days took the 840,000 km long journey. It was the world's first mission of its kind since almost 40 years, state media reported.
		  |After the US and the former Soviet Union, China is therefore the third country to which such a successful project. The test was in preparation for China's first lunar landing with subsequent return. In this planned for 2017 Flight China will not only put a probe on the Earth's satellite, but this then get back together with soil samples again to the earth.
		  |China is pushing its space program with great strides. The first space probe 'Chang'e 3 "had landed on 15 December 2013, the Moon and the vehicle had" Jadehase "exposed (Yutu). This was China after the United States and the Soviet Union the third nation in the world that has made a moon landing.
		  |The moon flights to demonstrate the technological capability of China's second largest economy""".stripMargin

	val tp = new TranslationProcess(textToImprove)

	//list type explicitly stated to speed up compilation (type inferencer). remove before production release
	val candidateProcessesParameterGenerators = Map(
		tp.SYNTAX_CHECK -> List[ParameterVariantGenerator[_]](
			new TypedParameterVariantGenerator[FFVSyntaxChecker](initWithDefaults = true)
				.addParameterVariations(FindFixVerifyProcess.FINDERS_COUNT.key, List(5, 7))
				.addParameterVariations(FindFixVerifyProcess.VERIFY_PROCESS.key, List(
				new ContestWithStatisticalReductionProcess(Map(
					ContestWithStatisticalReductionProcess.CONFIDENCE_PARAMETER.key -> 0.9 //inject processes to processes
				))
			))
		))
	val candidateProcesses = candidateProcessesParameterGenerators.map {
		//asinstanceof here is only used to speed up compilation (type inferencer). remove before production release
		case (key, generators) => (key, generators.map(_.generateVariationsAndInstanciate())
			.flatten.asInstanceOf[List[ProcessStub[_, _]]])
	}
	val candidateProcessCombinations = new RecombinationVariantGenerator(candidateProcesses).variants

	val trials = candidateProcessCombinations.map(c => {
		val timeBefore = System.currentTimeMillis()
		RecombinationStats(
			c,
			"", //tp.runRecombinedVariant(c),
			System.currentTimeMillis() - timeBefore,
			c.totalCost
		)
	})
	val fastestProcess = trials.minBy(_.processDuration).process
	println(s"the fastest process was $fastestProcess")
	println(s"we ran a total of ${trials.length} trials: ${trials.mkString("\n")}")
}

case class RecombinationStats(process: RecombinationVariant, result: String, processDuration: Long, processCost: Double)

class TranslationProcess(val textToImprove: String) extends Recombinable[String] {
	override def runRecombinedVariant(processes: RecombinationVariant): String = {
		val sentenceRewriter = processes.getProcess[List[String], List[String]](REWRITE_PART)
		val syntaxChecker = processes.getProcess[String, String](SYNTAX_CHECK)

		val paragraphs = textToImprove.split("\n").map(_.trim).toList

		val rewrittenPatches = sentenceRewriter.process(paragraphs)
		val textWithGoodSyntax = syntaxChecker.process(rewrittenPatches.mkString("\n"))

		textWithGoodSyntax
	}

	override def allRecombinationKeys: List[String] = List(REWRITE_PART, SYNTAX_CHECK)

	val REWRITE_PART = "1rewrite"
	val SYNTAX_CHECK = "2syntax"
}