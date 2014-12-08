package ch.uzh.ifi.pdeboer.pplib.process

import ch.uzh.ifi.pdeboer.pplib.process.stdlib.IterativeRefinementProcess
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 08/12/14.
 */
class ProcessMemoizerTest {

	@Test
	def testFinishedMemoizer: Unit = {
		val process = new IterativeRefinementProcess()
		val NAME: String = "patricks testprocess"
		val memoizer = new FileProcessMemoizer(process, NAME, true)
		val s = memoizer.addSnapshot("snapshot1").addElement("test", "blablupp").release()
		val s2 = memoizer.addSnapshot("snapshot2").addElement("test2", "bluppblupp").release()


		val mem2 = new FileProcessMemoizer(process, NAME, false)
		Assert.assertEquals(2, mem2.snapshots.size)
		val sFound = mem2.snapshots.find(_.name == "snapshot1").get
		Assert.assertEquals(s.dateCreated, sFound.dateCreated)
		Assert.assertEquals(s.name, sFound.name)
		Assert.assertEquals(s.elements, sFound.elements)

		val s2Found = mem2.snapshots.find(_.name == "snapshot2").get
		Assert.assertEquals(s2.dateCreated, s2Found.dateCreated)
		Assert.assertEquals(s2.name, s2Found.name)
		Assert.assertEquals(s2.elements, s2Found.elements)
	}

	@Test
	def testMem: Unit = {
		val process = new IterativeRefinementProcess()
		val memoizer = new FileProcessMemoizer(process, "asdf22", true)
		var counter: Int = 0
		val t = memoizer.mem("test") {
			counter += 1
			"asdf"
		}
		Assert.assertEquals(1, counter)
		val t2 = memoizer.mem("test") {
			counter += 1
			"asdf2"
		}
		Assert.assertEquals(1, counter)
		Assert.assertEquals("asdf", t2)
	}

	@Test
	def testMemStoreLoad: Unit = {
		val process = new IterativeRefinementProcess()
		val memoizer = new FileProcessMemoizer(process, "asdf22", true)
		var counter: Int = 0
		val t = memoizer.mem("test") {
			counter += 1
			"asdf"
		}

		val mem2 = new FileProcessMemoizer(process, "asdf22", false)
		val t2 = mem2.mem("test") {
			counter += 1
			"asdf2"
		}

		Assert.assertEquals(1, counter)
		Assert.assertEquals("asdf", t2)
	}

	@Test
	def testMemoizerException: Unit = {
		val process = new IterativeRefinementProcess()
		val mem = new FileProcessMemoizer(process, "asdf", true)
		val s = mem.addSnapshot("sn1").addElement("asdf", "asdf2").release()
		try {
			mem.addSnapshot("sn2").addElement("blablupp", "lol")
			throw new Exception("expected")
		}
		catch {
			case e: Exception => //hah! you were expected
		}
		Assert.assertEquals("sn1", mem.latest.get.name)

		val mem2 = new FileProcessMemoizer(process, "asdf", false)
		Assert.assertEquals("sn1", mem2.latest.get.name)
		Assert.assertEquals(s.dateCreated, mem2.latest.get.dateCreated)
		Assert.assertEquals(s.elements, mem2.latest.get.elements)
	}
}
