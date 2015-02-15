package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.hcomp._
import ch.uzh.ifi.pdeboer.pplib.process.parameter.Patch
import ch.uzh.ifi.pdeboer.pplib.process.{NoProcessMemoizer, ProcessMemoizer}
import ch.uzh.ifi.pdeboer.pplib.util.CollectionUtils._

import scala.util.Random

/**
 * Created by pdeboer on 10/12/14.
 */
class GeneticAlgorithmExecutor(@transient var driver: GeneticAlgorithmDriver,
							   @transient var terminationCriterion: GeneticAlgorithmTerminator,
							   val elitism: Double = 0.1,
							   val recombinationFraction: Double = 0.8,
							   val mutationFraction: Double = 0.1,
							   @transient memoizer: ProcessMemoizer = new NoProcessMemoizer(),
							   val memoizerPrefix: String = "") extends Serializable {

	private var populations: List[GAPopulation] = List(
		memoizer.mem(memoizerPrefix + "initialPop")(driver.initialPopulation)
	)

	lazy val refinedData = {
		do {
			populations = evolve :: populations
		} while (terminationCriterion.shouldEvolve(populations))
		latestChromosomes.map(_.patch)
	}

	def latestChromosomes = populations(0).chromosomes

	def evolve: GAPopulation = {
		val (elites: List[Patch], otherChromosomes: List[Patch]) = splitEliteAndNonElite(latestChromosomes)

		val newChromosomes = otherChromosomes.mpar.map(c => processPatch(c)).toList

		val allNewChromosomes = elites ::: newChromosomes
		val newPopulation = new GAPopulation(
			allNewChromosomes.mpar.map(c => new GAChromosome(c, driver.fitness(c))).toList)

		newPopulation
	}

	protected def processPatch(c: Patch): Patch = {
		memoizer.mem("patch_" + populations.size + "_" + c) {
			if (Random.nextDouble() <= recombinationFraction) {
				val otherParent = latestChromosomes(Random.nextInt(latestChromosomes.size))
				driver.combine(c, otherParent.patch)
			} else if (Random.nextDouble() <= mutationFraction) {
				driver.mutate(c)
			} else c
		}
	}

	protected def splitEliteAndNonElite(population: List[GAChromosome]): (List[Patch], List[Patch]) = {
		val elites = population.takeRight((population.size * elitism).toInt).map(_.patch)
		val otherChromosomes = population.take(population.size - elites.size).map(_.patch)
		(elites, otherChromosomes)
	}
}

trait GeneticAlgorithmDriver {
	def initialPopulation: GAPopulation

	def combine(patch1: Patch, patch2: Patch): Patch

	def mutate(patch: Patch): Patch

	def fitness(patch: Patch): Double

	def populationFromPatchList(patchList: List[Patch]) = new GAPopulation(patchList.map(d => new GAChromosome(d, 0)))
}

trait GeneticAlgorithmTerminator {
	def shouldEvolve(populations: List[GAPopulation]): Boolean
}

class GAChromosome(val patch: Patch, val fitness: Double) extends Comparable[GAChromosome] with Serializable {
	override def compareTo(o: GAChromosome): Int = fitness.compareTo(o.fitness)
}

class GAPopulation(_chromosomes: List[GAChromosome]) extends Serializable {
	val chromosomes = _chromosomes.sorted
}

class GAIterationLimitTerminator(val limit: Int = 10) extends GeneticAlgorithmTerminator {
	override def shouldEvolve(populations: List[GAPopulation]): Boolean = populations.size < limit
}

object GeneticAlgorithmHCompDriver {
	val DEFAULT_RATING_QUESTION: HCompInstructionsWithTupleStringified = new HCompInstructionsWithTupleStringified("Please rate the following paragraph in terms of syntax, its writing style, grammar and possible mistakes", questionAfterTuples = "Please do not accept more than 1 HIT in this group.")
	val DEFAULT_RATING_TITLE: String = "Please rate the following paragraph"
	val DEFAULT_MUTATE_TITLE: String = "Please refine the following paragraph"
	val DEFAULT_MUTATE_QUESTION: HCompInstructionsWithTupleStringified = new HCompInstructionsWithTupleStringified("Please refine the following paragraph", questionAfterTuples = "Please do not accept more than 1 HIT in this group.")
	val DEFAULT_COMBINE_TITLE: String = "Please combine the following two paragraphs"
	val DEFAULT_COMBINE_QUESTION = new HCompInstructionsWithTupleStringified("The following two paragraphs should be more or less equal. Please try to combine both of them and taking the best out of both. First paragraph: ", "Second paragraph:", "You can copy&paste the paragraph you like more into the field to begin with and augment it with elements of the other paragraph. Please do not accept more than 1 HIT in this group.")
	val DEFAULT_COST_PER_QUESTION = HCompQueryProperties()
}

class GeneticAlgorithmHCompDriver(val portal: HCompPortalAdapter,
								  val data: Patch,
								  val populationSize: Int = 10,
								  val combineQuestion: HCompInstructionsWithTupleStringified = GeneticAlgorithmHCompDriver.DEFAULT_COMBINE_QUESTION,
								  val combineTitle: String = GeneticAlgorithmHCompDriver.DEFAULT_COMBINE_TITLE,
								  val mutateQuestion: HCompInstructionsWithTupleStringified = GeneticAlgorithmHCompDriver.DEFAULT_MUTATE_QUESTION,
								  val mutateTitle: String = GeneticAlgorithmHCompDriver.DEFAULT_MUTATE_TITLE,
								  val ratingTitle: String = GeneticAlgorithmHCompDriver.DEFAULT_RATING_TITLE,
								  val ratingQuestion: HCompInstructionsWithTupleStringified = GeneticAlgorithmHCompDriver.DEFAULT_RATING_QUESTION,
								  val costPerQuestion: HCompQueryProperties = GeneticAlgorithmHCompDriver.DEFAULT_COST_PER_QUESTION
									 ) extends GeneticAlgorithmDriver {
	override def initialPopulation: GAPopulation = populationFromPatchList(
		(1 to populationSize).mpar.map(d => mutate(data)).toList)

	override def combine(patch1: Patch, patch2: Patch): Patch =
		patch1.duplicate(portal.sendQueryAndAwaitResult(
			new FreetextQuery(combineQuestion.getInstructions(patch1 + "", patch2 + ""), "", combineTitle), costPerQuestion)
			.get.is[FreetextAnswer].answer)


	override def mutate(patch: Patch): Patch = patch.duplicate(portal.sendQueryAndAwaitResult(
		new FreetextQuery(mutateQuestion.getInstructions(patch + ""), "", mutateTitle), costPerQuestion).get
		.is[FreetextAnswer].answer)

	override def fitness(patch: Patch): Double = {
		val options = List("Very good", "good", "bad", "very bad").reverse
		val answer = portal.sendQueryAndAwaitResult(
			MultipleChoiceQuery(ratingQuestion.getInstructions(patch + ""), options, 1, 1, ratingTitle)).get.asInstanceOf[MultipleChoiceAnswer].selectedAnswer
		val index = options.indexOf(answer) + 1d

		index / options.length.toDouble
	}
}