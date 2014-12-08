package ch.uzh.ifi.pdeboer.pplib.process.recombinationdb

import ch.uzh.ifi.pdeboer.pplib.process.{PPLibProcess, ProcessDB, ProcessStub, RecombinationCategory}
import ch.uzh.ifi.pdeboer.pplib.util.U
import org.junit.{Assert, Before, Test}

/**
 * Created by pdeboer on 10/11/14.
 */
class ProcessDBTest {
	@Test
	def testGetSameClass: Unit = {
		val category: RecombinationCategory = RecombinationCategory.get[String, A]("test.bla")
		val stub: TestProcessStubParent = new TestProcessStubParent()
		ProcessDB.put(
			stub //should be added on instance-creation
		)

		Assert.assertTrue(ProcessDB.getCategory(category, includeDescendants = false).head == stub)
	}

	@Test
	def testGetSameClassWrongType: Unit = {
		val category: RecombinationCategory = RecombinationCategory.get[String, String]("test.bla")
		val stub: TestProcessStubParent = new TestProcessStubParent()
		ProcessDB.put(
			stub
		)

		Assert.assertEquals("should not find any results",
			0,
			ProcessDB.getCategory(category, includeDescendants = false).size)
	}

	@Test
	def testGetSameClassAnnotation: Unit = {
		val category: RecombinationCategory = RecombinationCategory.get[String, A]("test.bla2")
		val stub = new TestProcessStubParent2()
		ProcessDB.put(
			stub //should be added on instance-creation
		)

		val search = ProcessDB.getCategory(category, includeDescendants = false)
		Assert.assertTrue(search.head == stub)
		Assert.assertEquals(1, search.size)
	}

	@Test
	def testChildClassRetrieving: Unit = {
		val needle: RecombinationCategory = RecombinationCategory.get[String, A]("test.")
		List(new TestProcessStubParent(), new TestProcessStubParent2(),
			new TestProcessStubChild(), new TestProcessStubUnrelated()).foreach(s => ProcessDB.put(s))

		val search = ProcessDB.getCategory(needle, includeDescendants = true)
		Assert.assertEquals(3, search.size)
	}

	@Test
	def testSetCharacterOfCategory: Unit = {
		val needle = RecombinationCategory.get[String, B]("test.")
		List(new TestProcessStubChild(Map("test" -> "a")), new TestProcessStubChild(Map("test" -> "a")),
			new TestProcessStubChild(Map("test" -> "b"))
		).foreach(c => ProcessDB.put(c))

		Assert.assertEquals("we try to insert the same stub (in terms of parameterset) twice, so only 1 of them should prevail",
			2, ProcessDB.getCategory(needle, includeDescendants = true).size)
	}

	@Test
	def testRetrieveDistinctClass: Unit = {
		val needle = RecombinationCategory.get[String, B]("test.")
		List(new TestProcessStubChild(Map("test" -> "a")), new TestProcessStubChild(Map("test" -> "b"))
		).foreach(c => ProcessDB.put(c))

		Assert.assertEquals("retrieving with distinct enabled. Only 1 class should be found",
			1, ProcessDB.getCategory(needle, includeDescendants = true, distinctProcesses = true).size)
	}

	@Test
	def testAnnotatedClassFinder: Unit = {
		val c = U.findClassesInPackageWithProcessAnnotation(this.getClass.getPackage.getName, classOf[PPLibProcess])
		Assert.assertEquals(Set(classOf[TestProcessStubParent2], classOf[TestProcessStubChild], classOf[TestProcessStubUnrelated]), c)
	}

	@Before
	def setUp: Unit = {
		ProcessDB.reset()
	}

	private class A(val a: String = "")

	private class B(val b: String = "") extends A(b)

	private class C(val c: String = "")

	private class TestProcessStubParent(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, A]() {
		override protected def run(data: String): A = {
			new A(data)
		}

		override protected def processCategoryNames: List[String] = List("test.bla")
	}

	@PPLibProcess("test.bla2")
	private class TestProcessStubParent2(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, A](params) {
		override protected def run(data: String): A = {
			new A(data)
		}
	}

	@PPLibProcess("test.bla3")
	private class TestProcessStubChild(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, B](params) {
		override protected def run(data: String): B = {
			new B(data)
		}
	}


	@PPLibProcess("test.bla4")
	private class TestProcessStubUnrelated(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, C](params) {
		override protected def run(data: String): C = {
			new C(data)
		}
	}

}
