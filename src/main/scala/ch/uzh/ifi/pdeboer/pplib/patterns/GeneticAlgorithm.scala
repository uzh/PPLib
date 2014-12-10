package ch.uzh.ifi.pdeboer.pplib.patterns

import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch
import ch.uzh.ifi.pdeboer.pplib.process.{NoProcessMemoizer, ProcessMemoizer}

import scala.util.Random

/**
 * Created by pdeboer on 10/12/14.
 */
class GeneticAlgorithmExecutor(@transient val driver: GeneticAlgorithmDriver,
							   @transient val terminationCriterion: GeneticAlgorithmTerminator,
							   val elitism: Double = 0.1,
							   val recombinationFraction: Double = 0.8,
							   val mutationFraction: Double = 0.1,
							   @transient memoizer: ProcessMemoizer = new NoProcessMemoizer()) extends Serializable {

	private var populations: List[GAPopulation] = List(driver.initialPopulation)

	lazy val refinedData = {
		do {
			populations = evolve :: populations
		} while (terminationCriterion.shouldEvolve(populations))
		latestChromosomes.map(_.patch)
	}

	def latestChromosomes = populations(0).chromosomes

	def evolve: GAPopulation = {
		val (elites: List[Patch], otherChromosomes: List[Patch]) = splitEliteAndNonElite(latestChromosomes)

		val newChromosomes = otherChromosomes.map(c => processPatch(c))

		val allNewChromosomes = elites ::: newChromosomes
		val newPopulation = new GAPopulation(
			allNewChromosomes.map(c => new GAChromosome(c, driver.fitness(c))))

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
}

trait GeneticAlgorithmTerminator {
	def shouldEvolve(populations: List[GAPopulation]): Boolean
}

class GAIterationLimitTerminator(val limit: Int = 10) extends GeneticAlgorithmTerminator {
	override def shouldEvolve(populations: List[GAPopulation]): Boolean = populations.size < limit
}


class GAChromosome(val patch: Patch, val fitness: Double) extends Comparable[GAChromosome] {
	override def compareTo(o: GAChromosome): Int = fitness.compareTo(o.fitness)
}

class GAPopulation(_chromosomes: List[GAChromosome]) {
	val chromosomes = _chromosomes.sorted
}