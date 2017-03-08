package ch.uzh.ifi.pdeboer.pplib.examples.openinnovation

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.util.U

import scala.util.Random

/**
  * Created by pdeboer on 08.03.17.
  */
object PowerPlantTest1 extends App {
  val portal = HComp.mechanicalTurk
  portal.approveAll = false
  U.initDBConnection()
  val decoratedPortal = new MySQLDBPortalDecorator(new RejectMultiAnswerHCompPortal(portal), None)

  val globalInstructions =
    """
      |<p>Imagine you are running a couple of power plants in the US, and you are deciding whether to build an additional power plant.
      |Assuming you already know the construction cost for the power plant, there are some ongoing cost that differ by power plant type (water, coal, solar..) and region (hot / cold, river/ocean close by etc).
      |In this HIT, we are trying to figure out ideas for <b>ongoing cost</b> that are to be expected (i.e. those that accumulate after building the plant) such that we can decide what type of plant to build, and where (by weighing different options).</p>
      |<p>The most 5 creative (and useful) answers for our HIT’s will be bonused $5.</p>
      |<p>Feel free to give multiple answers in this HIT, we will review all of them individually to nominate the winners.
      |Please collect all of your answers in one HIT and <b>do not submit more than one HIT of this type</b> (your 2nd, 3rd etc submission will be rejected, please submit only one per day of this type). You do not need to write something in each text field (just write 'nothing' where you have no idea), these are just to help you think of differences - completely empty / too trivial answers will be rejected though.</p>
    """.stripMargin

  def individualInstructions(plantType: String, region: String) = s"What are typical ongoing cost of $plantType power plants? You might also want to consider locality (cities vs desert vs island (e.g. Hawaii) vs rural etc)"

  val types = List("nuclear", "coal", "water", "biowaste", "solar")
  val regions = List("on an island (e.g. Hawaii)", "in a rural area (e.g. Iowa)", "in the desert")

  val emailField = FreetextQuery("If you're interested in participating in a follow-up workshop with higher payment outside MTurk, please tell us your email")

  private val allQueries = types.flatMap(t => regions.map(r => FreetextQuery(individualInstructions(t, r))))

  import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

  val data = (1 to 10).mpar.map(i => {
    val composite = CompositeQuery(Random.shuffle(allQueries).take(5) ::: List(emailField), globalInstructions, "Can you predict the cost of running a power plant?")
    decoratedPortal.sendQueryAndAwaitResult(composite, HCompQueryProperties(15))
  })
  println(data)
}
