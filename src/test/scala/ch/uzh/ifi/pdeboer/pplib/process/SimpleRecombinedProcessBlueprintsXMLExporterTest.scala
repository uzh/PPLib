package ch.uzh.ifi.pdeboer.pplib.process

import ch.uzh.ifi.pdeboer.pplib.process.entities.{ProcessStub, PassableProcessParam}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.SimpleRecombinationVariantXMLExporter
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 29/12/14.
 */
class SimpleRecombinedProcessBlueprintsXMLExporterTest {
	@Test
	def recursionShouldWork: Unit = {
		val exporter = new SimpleRecombinationVariantXMLExporter(null)
		val res = exporter.passableToXML(createNestedPassableProcess(3), 5)
		Assert.assertEquals(4, (res \\ "Key").length)
	}

	@Test
	def recursionShouldWork5: Unit = {
		val exporter = new SimpleRecombinationVariantXMLExporter(null)
		val res = exporter.passableToXML(createNestedPassableProcess(6), 6)
		Assert.assertEquals(7, (res \\ "Key").length)
	}

	@Test
	def recursionShouldBeLimited: Unit = {
		val exporter = new SimpleRecombinationVariantXMLExporter(null)
		val res = exporter.passableToXML(createNestedPassableProcess(6), 5)
		Assert.assertEquals(6, (res \\ "Key").length)
	}

	@Test
	def recursionShouldBeLimited2: Unit = {
		val exporter = new SimpleRecombinationVariantXMLExporter(null)
		val res = exporter.passableToXML(createNestedPassableProcess(5), 5)
		Assert.assertEquals(6, (res \\ "Key").length)
	}

	def createNestedPassableProcess(children: Int): PassableProcessParam[ProcessStub[Int, Int]] = {
		new PassableProcessParam[ProcessStub[Int, Int]](Map(("subprocess",
			if (children > 0) createNestedPassableProcess(children - 1) else "naaah")
		))
	}
}
