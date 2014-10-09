package ch.uzh.ifi.pdeboer.crowdlang.workflow

import org.junit.{Assert, Test}

/**
 * Created by pdeboer on 09/10/14.
 */
class BranchOutTest {
  @Test
  def testBranchOut(): Unit = {
    val data = "a,b,c,d"

    val bo = new BranchOut(
      (s: String) => s.split(",").toList,
      (s: String) => (Char.char2float(s.charAt(0)).toInt + 1).toChar,
      (i: List[Char]) => i.mkString(",")
    )

    Assert.assertEquals("b,c,d,e", bo.apply(data))
  }
}
