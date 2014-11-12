package ch.uzh.ifi.pdeboer.pplib.recombination.recombinationdb

import ch.uzh.ifi.pdeboer.pplib.recombination.{RecombinationCategory, RecombinationDB, RecombinationProcess, ProcessStub}
import org.junit.{Assert, Before, Test}

/**
 * Created by pdeboer on 10/11/14.
 */
class RecombinationDBTest {
	@Test
	def testGetSameClass: Unit = {
		val category: RecombinationCategory = RecombinationCategory.get[String, A]("test.bla")
		val stub: TestProcessStubParent = new TestProcessStubParent()
		RecombinationDB.put(
			stub //should be added on instance-creation
		)

		Assert.assertTrue(RecombinationDB.getCategory(category, includeDescendants = false)(0) == stub)
	}

	@Test
	def testGetSameClassWrongType: Unit = {
		val category: RecombinationCategory = RecombinationCategory.get[String, String]("test.bla")
		val stub: TestProcessStubParent = new TestProcessStubParent()
		RecombinationDB.put(
			stub
		)

		Assert.assertEquals("should not find any results",
			0,
			RecombinationDB.getCategory(category, includeDescendants = false).size)
	}

	@Test
	def testGetSameClassAnnotation: Unit = {
		val category: RecombinationCategory = RecombinationCategory.get[String, A]("test.bla2")
		val stub = new TestProcessStubParent2()
		RecombinationDB.put(
			stub //should be added on instance-creation
		)

		val search: List[ProcessStub[_, _]] = RecombinationDB.getCategory(category, includeDescendants = false)
		Assert.assertTrue(search(0) == stub)
		Assert.assertEquals(1, search.length)
	}

	@Test
	def testChildClassRetrieving: Unit = {
		val needle: RecombinationCategory = RecombinationCategory.get[String, A]("test.")
		List(new TestProcessStubParent(), new TestProcessStubParent2(),
			new TestProcessStubChild(), new TestProcessStubUnrelated()).foreach(s => RecombinationDB.put(s))

		val search: List[ProcessStub[_, _]] = RecombinationDB.getCategory(needle, includeDescendants = true)
		Assert.assertEquals(3, search.length)
	}

	@Test
	def testSetCharacterOfCategory: Unit = {
		val needle = RecombinationCategory.get[String, B]("test.")
		List(new TestProcessStubChild(Map("test" -> "a")), new TestProcessStubChild(Map("test" -> "a")),
			new TestProcessStubChild(Map("test" -> "b"))
		).foreach(c => RecombinationDB.put(c))

		Assert.assertEquals("we try to insert the same stub (in terms of parameterset) twice, so only 1 of them should prevail",
			2, RecombinationDB.getCategory(needle, includeDescendants = true).length)
	}

	@Test
	def testAnnotatedClassFinder: Unit = {
		val c = RecombinationDB.findClassesInPackageWithProcessAnnotation(this.getClass.getPackage.getName)
		Assert.assertEquals(Set(classOf[TestProcessStubParent2], classOf[TestProcessStubChild], classOf[TestProcessStubUnrelated]), c)
	}

	@Before
	def setUp: Unit = {
		RecombinationDB.reset()
	}

	private class A(val a: String = "")

	private class B(val b: String = "") extends A(b)

	private class C(val c: String = "")

	private class TestProcessStubParent(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, A]() {
		override protected def run(data: String): A = {
			new A(data)
		}

		override protected def recombinationCategoryNames: List[String] = List("test.bla")
	}

	@RecombinationProcess("test.bla2")
	private class TestProcessStubParent2(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, A](params) {
		override protected def run(data: String): A = {
			new A(data)
		}
	}

	@RecombinationProcess("test.bla3")
	private class TestProcessStubChild(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, B](params) {
		override protected def run(data: String): B = {
			new B(data)
		}
	}


	@RecombinationProcess("test.bla4")
	private class TestProcessStubUnrelated(params: Map[String, Any] = Map.empty[String, Any]) extends ProcessStub[String, C](params) {
		override protected def run(data: String): C = {
			new C(data)
		}
	}

}
