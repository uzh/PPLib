package ch.uzh.ifi.pdeboer.pplib.patterns

import java.io.{ByteArrayInputStream, ObjectInputStream, ByteArrayOutputStream, ObjectOutputStream}

import ch.uzh.ifi.pdeboer.pplib.hcomp.MockHCompPortal
import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 09/12/14.
 */
class FindFixVerifySerializationTest {
	@Test
	def testSerializationOfExecutor: Unit = {
		val portal = new MockHCompPortal
		val driver = new FFVDefaultHCompDriver(List.empty[FFVPatch[String]], portal, new FFVFindQuestion("findtest"), new FFVFixQuestion("fixtest"))
		val exec = new FindFixVerifyExecutor[String](driver, 9)

		val bos: ByteArrayOutputStream = new ByteArrayOutputStream()
		val oos = new ObjectOutputStream(bos)
		oos.writeObject(exec)
		oos.close()

		val ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray))
		val copy = ois.readObject().asInstanceOf[FindFixVerifyExecutor[String]]

		Assert.assertEquals(exec.maxPatchesCountInFind, copy.maxPatchesCountInFind)
	}
}
