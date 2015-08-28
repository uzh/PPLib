package ch.uzh.ifi.pdeboer.pplib.process.entities

/**
 * Created by pdeboer on 13/02/15.
 */
trait IParametrizable {

	import ch.uzh.ifi.pdeboer.pplib.process.entities.DefaultParameters._

	def expectedParametersOnConstruction: List[ProcessParameter[_]] = List.empty[ProcessParameter[_]]

	def expectedParametersBeforeRun: List[ProcessParameter[_]] = List.empty[ProcessParameter[_]]

	def optionalParameters: List[ProcessParameter[_]] = List.empty[ProcessParameter[_]]

	def defaultParameters: List[ProcessParameter[_]] = List(MEMOIZER_NAME, STORE_EXECUTION_RESULTS)

	def processParameterDefaults: Map[ProcessParameter[_], List[Any]] = Map.empty

	def combineParameterLists(paramsToAdd: List[ProcessParameter[_]], existingParameters: List[ProcessParameter[_]]): List[ProcessParameter[_]] = {
		val existingKeys: List[String] = existingParameters.map(_.key)
		val nonExisting = paramsToAdd.filterNot(p => existingKeys.contains(p.key))
		nonExisting ::: existingParameters
	}
}
