package ch.uzh.ifi.pdeboer.crowdlang.recombination

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 10/10/14.
 */
class RecombinationVariantGeneratorTest {
  private class TestRecombinationStub(val id:String) extends RecombinationStub[String,String,String,String](f=>id,f=>id,f=>id)

  @Test
  def testSimpleCombination(): Unit = {
    val list1 = List(new TestRecombinationStub("1"), new TestRecombinationStub("2"))
    val list2 = List(new TestRecombinationStub("3"))
    val list3 = List(new TestRecombinationStub("4"), new TestRecombinationStub("5"))

    val configMap = List(("list1",list1), ("list2",list2) , ("list3",list3)).toMap

    val gen = new RecombinationVariantGenerator(configMap)
    Assert.assertTrue(gen.variants.size > 0)
  }
}
