package ch.uzh.ifi.pdeboer.pplib.examples.recombination.translation

import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp
import ch.uzh.ifi.pdeboer.pplib.hcomp.mturk.MechanicalTurkPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.recombination.{RecombinationVariant, RecombinationVariantXMLExporter}

import scala.xml.XML

/**
 * Created by pdeboer on 01/12/14.
 */
object TranslationApp extends App {
	HComp(MechanicalTurkPortalAdapter.PORTAL_KEY).setBudget(Some(100))

	val recombinations: List[RecombinationVariant] = TranslationRecombination.recombinations
	private val textToImprove: String =
		"""China has for the first time sent a probe to the moon and back to Earth. The return capsule landed after a circumlunar flight of the orbiter on Saturday morning in Mongolia. Eight days took the 840,000 km long journey. It was the world's first mission of its kind since almost 40 years, state media reported.
		  |After the US and the former Soviet Union, China is therefore the third country to which such a successful project. The test was in preparation for China's first lunar landing with subsequent return. In this planned for 2017 Flight China will not only put a probe on the Earth's satellite, but this then get back together with soil samples again to the earth.
		  |China is pushing its space program with great strides. The first space probe 'Chang'e 3 "had landed on 15 December 2013, the Moon and the vehicle had" Jadehase "exposed (Yutu). This was China after the United States and the Soviet Union the third nation in the world that has made a moon landing.
		  |The moon flights to demonstrate the technological capability of China's second largest economy""".stripMargin

	val process = new TranslationProcess(textToImprove)
	val data = recombinations.map(r => (r, process.runRecombinedVariant(r)))

	val xml = <ProcessData>
		{data.map(v => {
			<ExecutedVariation>
				<Result>
					{v._2}
				</Result>{new RecombinationVariantXMLExporter(v._1).xml}
			</ExecutedVariation>
		})}
	</ProcessData>

	XML.save("variations.xml", xml)
	println("finished " + xml)
}
