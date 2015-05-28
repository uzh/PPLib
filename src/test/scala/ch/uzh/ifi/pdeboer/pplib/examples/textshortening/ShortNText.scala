package ch.uzh.ifi.pdeboer.pplib.examples.textshortening

import ch.uzh.ifi.pdeboer.pplib.process.recombination.Recombinator
import ch.uzh.ifi.pdeboer.pplib.util.ProcessPrinter

import scala.reflect.io.File


/**
 * Created by pdeboer on 12/05/15.
 */
object ShortNText extends App {
	val testData = new ShortNTestDataInitializer()
	testData.initializePortal()

	val surfaceStructure = new ShortNSurfaceStructure(testData.text)

	val recombinations = new Recombinator(surfaceStructure).recombine()
	println(s"generated ${recombinations.size} recombinations. running evaluation..")

	val results = recombinations.par.map(variant => {
		(variant, surfaceStructure.runRecombinedVariant(variant))
	})

	println("finished evaluation.")
	println(s"shortest result: ${results.minBy(_._2.length)}")

	val lines = results.flatMap {
		case (variant, result) => variant.stubs.map {
			case (stubKey, process) => s"$stubKey => ${new ProcessPrinter(process, Some(Nil))}"
		}
	}
	println("writing generated recombinations and their results into an XML..")
	File("test.xml").writeAll(lines.mkString("\n"))

	println("all done")
}
