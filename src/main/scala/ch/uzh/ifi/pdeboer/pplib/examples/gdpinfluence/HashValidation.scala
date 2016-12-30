package ch.uzh.ifi.pdeboer.pplib.examples.gdpinfluence

import java.security.MessageDigest

/**
  * Created by marcello on 30/12/16.
  */
object HashValidation extends App{

  def extractToken(original:String): String = {
    val index = original.indexOf('|')
    return original.substring(index+1)
  }

  /**
    * validate("0:7,1:3,2:5,3:8,4:4,5:9,6:14,7:13,8:1,9:2,10:10,11:11,12:6,13:15,14:12|7c19af4c1a89f83ecb44e43b3b5b92f1", "Mr Peter Test")
    * @param token token from answer (including ranking string. Part after "|" should be hashed name)
    * @param original name / random String
    * @return boolean true if is valid else false
    */
  def validateToken(token:String, original:String): Boolean ={
    val givenHash = extractToken(token)
    val trueHash = md5(original).map("%02X".format(_)).mkString.toLowerCase()
    return trueHash == givenHash.toLowerCase()
  }

  def md5(s: String) = {
    MessageDigest.getInstance("MD5").digest(s.getBytes)
  }

}
