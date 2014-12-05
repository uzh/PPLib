package ch.uzh.ifi.pdeboer.pplib.process

import ch.uzh.ifi.pdeboer.pplib.util.U
import org.junit.{Assert, Test}

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 28/11/14.
 */
class RecombinationVariantXMLExporterTest {
	@Test
	def testStringListExport: Unit = {
		val inputDataProcess: List[String] = List("test1", "test2")
		val outputDataProcess: String = "result1"
		val process = new TestProcess[List[String], String](Map(inputDataProcess -> outputDataProcess))
		val variant = new RecombinationVariant(Map("testprocess" -> process))

		val exporter = new RecombinationVariantXMLExporter(variant) //List is default
		val xmlExport = exporter.transformDataWithExporter(process, process.inputType.runtimeClass, inputDataProcess)

		Assert.assertEquals("<List><Item>test1</Item><Item>test2</Item></List>",
			U.removeWhitespaces(xmlExport.toString))

		Assert.assertEquals(outputDataProcess, exporter.transformDataWithExporter(process, process.outputType.runtimeClass, outputDataProcess))
	}

	@Test
	def testStringSetExport: Unit = {
		val inputDataProcess = Set("test1", "test2")
		val outputDataProcess: String = "result1"
		val process = new TestProcess[Set[String], String](Map(inputDataProcess -> outputDataProcess))
		val variant = new RecombinationVariant(Map("testprocess" -> process))

		val exporter = new RecombinationVariantXMLExporter(variant) //List is default
		val xmlExport = exporter.transformDataWithExporter(process, process.inputType.runtimeClass, inputDataProcess)

		Assert.assertEquals("<Set><Item>test1</Item><Item>test2</Item></Set>",
			U.removeWhitespaces(xmlExport.toString))

		Assert.assertEquals(outputDataProcess, exporter.transformDataWithExporter(process, process.outputType.runtimeClass, outputDataProcess))
	}

	@Test
	def testIntListExport: Unit = {
		val inputDataProcess = List(1, 2, 3)
		val outputDataProcess = 3
		val process = new TestProcess[List[Int], Integer](Map(inputDataProcess -> outputDataProcess))
		val variant = new RecombinationVariant(Map("testprocess" -> process))

		val exporter = new RecombinationVariantXMLExporter(variant) //List is default
		val xmlExport = exporter.transformDataWithExporter(process, process.inputType.runtimeClass, inputDataProcess)

		Assert.assertEquals("<List><Item>1</Item><Item>2</Item><Item>3</Item></List>",
			U.removeWhitespaces(xmlExport.toString))

		Assert.assertEquals(outputDataProcess, exporter.transformDataWithExporter(process, process.outputType.runtimeClass, outputDataProcess))
	}


	@Test
	def testMapExport: Unit = {
		val inputDataProcess = Map("key1" -> "value1", "key2" -> "value2")
		val outputDataProcess = 3
		val process = new TestProcess[Map[String, String], Integer](Map(inputDataProcess -> outputDataProcess))
		val variant = new RecombinationVariant(Map("testprocess" -> process))

		val exporter = new RecombinationVariantXMLExporter(variant) //List is default
		val xmlExport = exporter.transformDataWithExporter(process, process.inputType.runtimeClass, inputDataProcess)

		Assert.assertEquals("<Map><Item><Key>key1</Key><Value>value1</Value></Item><Item><Key>key2</Key><Value>value2</Value></Item></Map>",
			U.removeWhitespaces(xmlExport.toString))

		Assert.assertEquals(outputDataProcess, exporter.transformDataWithExporter(process, process.outputType.runtimeClass, outputDataProcess))
	}

	@Test
	def testCompleteExport: Unit = {
		val inputDataProcess: List[String] = List("test1", "test2")
		val outputDataProcess: String = "result1"
		val process = new TestProcess[List[String], String](Map(inputDataProcess -> outputDataProcess))
		val variant = new RecombinationVariant(Map("testprocess" -> process))

		val exporter = new RecombinationVariantXMLExporter(variant) //List is default
		Assert.assertEquals("<Variant><ProcessExecutions><ProcessExecution><Name>testprocess</Name><Process><Class>ch.uzh.ifi.pdeboer.pplib.process.RecombinationVariantXMLExporterTest$TestProcess</Class><InputClass>scala.collection.immutable.List</InputClass><OutputClass>java.lang.String</OutputClass><Categories><Category></Category></Categories><Parameters></Parameters></Process><Results><Result><Input><List><Item>test1</Item><Item>test2</Item></List></Input><Output>result1</Output></Result></Results></ProcessExecution></ProcessExecutions></Variant>", U.removeWhitespaces(exporter.xml + ""))
	}


	private class TestProcess[IN: ClassTag, OUT: ClassTag](val runs: Map[IN, OUT]) extends ProcessStub[IN, OUT]() {
		//not needed. Yes, it's ugly
		override protected def run(data: IN): OUT = ???

		override def results: Map[IN, OUT] = runs
	}

}
