package ch.uzh.ifi.pdeboer.pplib.patterns

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import ch.uzh.ifi.pdeboer.pplib.hcomp.{CostCountingEnabledHCompPortal, HComp, MockHCompPortal}
import ch.uzh.ifi.pdeboer.pplib.process.FileProcessMemoizer
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 09/12/14.
 */
class FindFixVerifySerializationTest {
	val portal = new MockHCompPortal

	@Test
	def testSerializationOfExecutor: Unit = {
		val driver = new FFVDefaultHCompDriver(List.empty[FFVPatch[String]], portal, new FFVFindQuestion("findtest"), new FFVFixQuestion("fixtest"))
		val exec = new FindFixVerifyExecutor[String](driver, 9)

		val copy: FindFixVerifyExecutor[String] = serializeAndDeserialize(exec)

		Assert.assertEquals(exec.maxPatchesCountInFind, copy.maxPatchesCountInFind)
	}

	def serializeAndDeserialize(exec: FindFixVerifyExecutor[String]): FindFixVerifyExecutor[String] = {
		val bos: ByteArrayOutputStream = new ByteArrayOutputStream()
		val oos = new ObjectOutputStream(bos)
		oos.writeObject(exec)
		oos.close()

		val ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray))
		val copy = ois.readObject().asInstanceOf[FindFixVerifyExecutor[String]]
		copy
	}

	@Test
	def testSerializationOfFFVProcess: Unit = {
		val driver = new FFVDefaultHCompDriver(
			List("asdf1", "asdf2").zipWithIndex.map(d => FFVPatch[String](d._1, d._2)),
			new CostCountingEnabledHCompPortal(HComp.mechanicalTurk), new FFVFindQuestion("asdf?"), new FFVFixQuestion("asdf?"),
			"ftitle", "fixtitle", FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS,
			FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS_CONTEXT_PARAMETER, FFVDefaultHCompDriver.DEFAULT_VERIFY_PROCESS_CONTEXT_FLATTENER,
			true
		)

		val memoizer = new FileProcessMemoizer("test", true)
		val exec = memoizer.memWithReinitialization("ffvexec")(
			new FindFixVerifyExecutor(driver, _memoizer = memoizer, maxPatchesCountInFind = 99))(exec => {
			exec.driver = driver
			exec.memoizer = memoizer
			exec
		})

		val mem2 = new FileProcessMemoizer("test")

		val exec2 = mem2.memWithReinitialization("ffvexec") {
			throw new IllegalStateException("ffvexec should be loaded")
			new FindFixVerifyExecutor(driver, _memoizer = mem2)
		}(exec => {
			exec.driver = driver
			exec.memoizer = mem2
			println("loaded ffvexec")
			exec
		})

		Assert.assertEquals(exec.maxPatchesCountInFind, exec2.maxPatchesCountInFind)
	}
}
