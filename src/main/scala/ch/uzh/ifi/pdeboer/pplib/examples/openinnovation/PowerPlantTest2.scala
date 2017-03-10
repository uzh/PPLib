package ch.uzh.ifi.pdeboer.pplib.examples.openinnovation

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.util.U

/**
  * Created by pdeboer on 08.03.17.
  */
object PowerPlantTest2 extends App {
  val portal = HComp.mechanicalTurk
  portal.approveAll = false
  U.initDBConnection()
  val decoratedPortal = new MySQLDBPortalDecorator(new RejectMultiAnswerHCompPortal(portal), None)


  private val plants = List(
    """<a href="https://en.wikipedia.org/wiki/New_Melones_Dam">New_Melones_Dam</a> and <a href="https://en.wikipedia.org/wiki/Moss_Landing_Power_Plant">Moss_Landing_Power_Plant</a></p>""",
    """<a href="https://en.wikipedia.org/wiki/George_E._Turner_Power_Plant">George_E._Turner_Power_Plant</a> and <a href="https://en.wikipedia.org/wiki/Windy_Point/Windy_Flats">Windy_Point/Windy_Flats</a></p>""",
    """<a href="https://en.wikipedia.org/wiki/Brayton_Point_Power_Station">Brayton Power Station</a> and <a href="https://en.wikipedia.org/wiki/Cimarron_Solar_Facility>Cimarron Solar</a>""")

  val composites = plants.map(powerPlantPair => {
    val globalInstructions =
      """<p>We are trying to better understand the costs of running power plants. To do so, we are asking you to do some thinking (maybe also some reading :) ).
        |We are comparing the following two power plants in this HIT: <br/>
      """ + powerPlantPair +
        """
          |<p>The most 5 creative (and useful) answers for our HIT’s will be bonused $5. (Note, that we are looking for indirect/direct cost factors such as '<i>uranium disposal cost</i>' (more creative would be better), NOT the number)</p>
          |<p>Feel free to give multiple answers in this HIT, we will review all of them individually to nominate the winners.
          |Please collect all of your answers in one HIT and <b>do not submit more than one HIT of this type</b> (your 2nd, 3rd etc submission will be rejected, please submit only one per day of this type). You do not need to write something in each text field (just write 'nothing' where you have no idea), these are just to help you think of differences - completely empty / too trivial answers will be rejected though.</p>
        """.stripMargin

    val majorCost = FreetextQuery(globalInstructions + "<p>What do you think are the major cost factors of each power plant?</p>")
    val differentCost = FreetextQuery("Name three cost factors that are different between these two power plants")
    val othersCost = FreetextQuery("Name three cost factors that you think most people will not think of while contemplating about the cost of each one of these power plants")
    val resources = FreetextQuery("Please refer us to the sources you find useful")
    val emailField = FreetextQuery("If you're interested in participating in a follow-up workshop with higher payment outside MTurk, please tell us your email (optional, put 'no' if not interested)")

    CompositeQuery(List(majorCost, differentCost, othersCost, resources, emailField), globalInstructions, "Can you predict the cost of running a power plant?")
  })

  import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

  val data = (1 to 3).mpar.flatMap(i => {
    composites.mpar.map(composite => decoratedPortal.sendQueryAndAwaitResult(composite, HCompQueryProperties(20)))
  })
  println(data)

}
