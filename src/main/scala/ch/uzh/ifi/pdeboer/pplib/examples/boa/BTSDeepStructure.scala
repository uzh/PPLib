package ch.uzh.ifi.pdeboer.pplib.examples.boa

import java.io.File

import ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal.MySQLDBPortalDecorator
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HComp, HCompPortalAdapter, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation.NaiveAutoExperimentationEngine
import ch.uzh.ifi.pdeboer.pplib.process.entities.{InstructionData, _}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{AddedParameterRecombinationHint, _}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib._
import com.github.tototoshi.csv.CSVReader

import scala.util.Random

object BTSExperiment extends App {
	val EXPERIMENT_SIZE = 2
	val targetStates = BTSResult.stateToCities.keys.toList.sortBy(r => Random.nextDouble()).take(EXPERIMENT_SIZE)

	private val portal: HCompPortalAdapter = new MySQLDBPortalDecorator(HComp.mechanicalTurk)
	//new BTSTestPortal()
	val deepStructure = new BTSDeepStructure(portal)
	private val recombinator: Recombinator[List[String], BTSResult] = new Recombinator(deepStructure)
	val recombinations = recombinator.recombine
	println(s"generated ${recombinations.size} recombinations. running evaluation..")

	val targetRecombinations = recombinator.sneakPeek

	//private val onlyTruthContest = targetRecombinations.filter(_.recombinedProcessBlueprint.stubs.values.head.baseType.tpe <:< typeOf[BayesianTruthContest])
	val autoExperimentation = new NaiveAutoExperimentationEngine(targetRecombinations)
	val results = autoExperimentation.runOneIteration(targetStates)
	//val results = autoExperimentation.run(targetStates, 50, memoryFriendly = true)

	println("finished evaluation.")
	val expander = new SurfaceStructureFeatureExpander[List[String], BTSResult](results.surfaceStructures.toList)
	val targetFeatures = expander.featuresInclClass.filter(f => List("TypeTag[Int]", "TypeTag[Double]", XMLFeatureExpander.baseClassFeature.typeName).contains(f.typeName)).toList
	expander.toCSV("btsresultsModel.csv", targetFeatures, results.surfaceStructures.map(ss => ss ->
		results.resultsForSurfaceStructure(ss).zipWithIndex
			.map(r => (r._2 + "_result") -> r._1.result.get.costFunctionResult).toMap
	).toMap)

	println(s"best result: ${results.bestProcess}")
}

class BTSResult(selectedCitiesForStates: Map[String, String], processCostInCents: Int) extends ResultWithCostfunction {
	override def costFunctionResult: Double = {
		val correct = selectedCitiesForStates.map(kv => if (BTSResult.groundTruth(kv._1) == kv._2) 1 else 0).sum
		val wrongClassifications: Int = selectedCitiesForStates.size - correct
		val LOSS_FROM_WRONG_CLASSIFICATION_IN_CENTS: Int = 300

		wrongClassifications * LOSS_FROM_WRONG_CLASSIFICATION_IN_CENTS + processCostInCents
	}
}

object BTSResult {
	lazy val groundTruth = {
		val reader = CSVReader.open(s"example_data${File.separator}us capitals.csv")
		val rawData = reader.allWithHeaders()
		rawData.map(rd => rd("state") -> rd("capital")).toMap
	}

	lazy val stateToCities = {
		val reader = CSVReader.open(s"example_data${File.separator}us capitals.csv")
		val rawData = reader.allWithHeaders()
		rawData.map(rd => rd("state") -> List(rd("capital"), rd("city1"), rd("city2"), rd("city3"))).toMap
	}
}

/**
  * Created by pdeboer on 18/03/16.
  */
class BTSDeepStructure(val portalToUse: HCompPortalAdapter) extends SimpleDeepStructure[List[String], BTSResult] {

	import scala.reflect.runtime.universe._

	override def run(data: List[String], blueprint: RecombinedProcessBlueprint): BTSResult = {

		type inputType = List[Patch]
		type outputType = Patch

		val res = data.map(state => {
			val cities = BTSResult.stateToCities(state).map(c => new Patch(c))

			val generatedShorteningProcess: ProcessStub[inputType, outputType] = blueprint.createProcess[inputType, outputType](forcedParams = Map(FixPatchProcess.ALL_DATA.key -> cities, DefaultParameters.INSTRUCTIONS_ITALIC.key -> state))
			val result = generatedShorteningProcess.process(cities)
			state ->(result.value, generatedShorteningProcess.asInstanceOf[HCompPortalAccess].portal.cost)
		})
		new BTSResult(res.map(r => r._1 -> r._2._1).toMap, res.map(_._2._2).sum)
	}

	override def defineSimpleRecombinationSearchSpace: RecombinationSearchSpaceDefinition[_ <: ProcessStub[_, _]] =
		RecombinationSearchSpaceDefinition[DecideProcess[_ <: List[Patch], _ <: Patch]](
			RecombinationHints.create(Map(
				RecombinationHints.DEFAULT_HINTS -> {
					new AddedParameterRecombinationHint[Int](DefaultParameters.MAX_ITERATIONS, 20 to 30) ::
						new AddedParameterRecombinationHint[Int](DefaultParameters.WORKER_COUNT, 3 to 9) ::
						new AddedParameterRecombinationHint[Int](ContestWithBeatByKVotingProcess.K, 1 to 10) ::
						new AddedParameterRecombinationHint[Double](ContestWithStatisticalReductionProcess.CONFIDENCE_PARAMETER, (1 to 8).map(i => 0.6 + (i.toDouble * .05))) ::
						RecombinationHints.hcompPlatform(List(portalToUse)) :::
						RecombinationHints.instructionPool(Map(
							typeOf[DecideProcess[_, _]] -> new TrivialInstructionGenerator("What is the capital of the state below?", "Please select the capital of this state", questionBetween = "Please select the city you think is the capital from the top of your head (no google) among the list below. "),
							typeOf[OtherOpinionsDecide] -> new TrivialInstructionGenerator("If other crowd workers were asked the same question. How likely is it, that they give the answer below?", "", "They would of course also be asked to identify the capital of")
						)) :::
						//RecombinationHints.questionPrice(List(HCompQueryProperties(0, qualifications = Nil))) :::
						RecombinationHints.instructions(List(
							new InstructionData(actionName = "the same question. How likely is it that they give the answer below?", detailedDescription = "identify the capital of")))
				}
				,
				classOf[BayesianTruthContest] ->
					RecombinationHints.questionPrice(List(HCompQueryProperties(15)))

			)
			)
		)
}
