package ch.uzh.ifi.pdeboer.pplib.examples.optimization

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import org.joda.time.DateTime

import scala.util.Random

/**
  * Created by pdeboer on 11/12/15.
  */
class MCOptimizationMockPortal extends HCompPortalAdapter {
	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		val answer = query match {
			case x: FreetextQuery =>
				throw new IllegalArgumentException("I can only process multiple choice queries")
			case x: MultipleChoiceQuery =>
				processMultipleChoiceQuery(x)
			case x: CompositeQuery => Some(CompositeQueryAnswer(x, x.queries.map(q => (q, processQuery(q, properties))).toMap))
		}

		//set times
		answer.map(a => {
			a.postTime = DateTime.now()
			a.acceptTime = Some(DateTime.now())
			a.submitTime = Some(DateTime.now())
			a.receivedTime = DateTime.now()

			a
		})
	}

	protected def processMultipleChoiceQuery(x: MultipleChoiceQuery): Option[MultipleChoiceAnswer] = {
		if (x.maxSelections != 1) throw new IllegalArgumentException("I can only deal with MC Questions that allow 1 selection")
		val randomlySortedOptions = x.options.sortBy(s => Random.nextDouble())
		val cdfOfOptions = randomlySortedOptions.zipWithIndex.map(t => randomlySortedOptions.take(t._2).map(_.toInt).sum + t._1.toInt)
		val targetRandom = Random.nextDouble() * 100
		val firstLargerElement = cdfOfOptions.zipWithIndex.find(i => i._1 > targetRandom)
		val indexOfTargetElement = if (firstLargerElement.isDefined) firstLargerElement.get._2 else cdfOfOptions.length - 1

		val selections = List(randomlySortedOptions(indexOfTargetElement))

		Some(MultipleChoiceAnswer(x, x.options.map(o => (o, selections.contains(o))).toMap))
	}

	override def getDefaultPortalKey: String = MCOptimizationMockPortal.PORTAL_KEY

	override def cancelQuery(query: HCompQuery): Unit = {}
}

object MCOptimizationMockPortal {
	val PORTAL_KEY = "MCOptimizationMockPortal"
}

