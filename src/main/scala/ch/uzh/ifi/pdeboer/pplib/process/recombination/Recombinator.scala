package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessParameter, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._


/**
 * Created by pdeboer on 27/03/15.
 */
class Recombinator(hints: RecombinationHints, db: RecombinationDB = RecombinationDB.DEFAULT) extends LazyLogger {
	def materialize[BASE <: ProcessStub[_, _]]()(implicit baseType: TypeTag[BASE], baseClass: ClassTag[BASE]): List[PassableProcessParam[BASE]] = {
		processRecombination(baseType.tpe, baseClass.runtimeClass.asInstanceOf[Class[BASE]]).asInstanceOf[List[PassableProcessParam[BASE]]]
	}

	def processRecombination(baseType: Type, baseClass: Class[_ <: ProcessStub[_, _]]): List[PassableProcessParam[ProcessStub[_, _]]] = {
		val applicableTypes = getApplicableTypesInDB(baseType)
		val recombinationsOfTheseTypes = applicableTypes.map { case (processType, processClass) => {
			//TODO check for circles
			val baseProcess: ProcessStub[_, _] = ProcessStub.createFromBlueprint(processClass, Map.empty)

			val variantGenerator = new InstanciatedParameterVariantGenerator[ProcessStub[_, _]](baseProcess)
			baseProcess.allParams.foreach(p =>
				variantGenerator.addVariation(p, generateCandidatesForParameter(baseProcess, processType, p))
			)
			variantGenerator.generatePassableProcesses()
		}
		}.flatten

		recombinationsOfTheseTypes
	}

	protected def generateCandidatesForParameter[T <: ProcessStub[_, _]](containerClass: T, containerType: Type, param: ProcessParameter[_]): List[Any] = {
		val allParamsAddedInHints = hints(containerClass.getClass).map(_.processConstructionParameter.getOrElse(param.key, List.empty[T])).flatten
		val allDefaultParamsInProcess = param.candidateDefinitions.getOrElse(Nil).toList

		val shouldRunComplexParamRecombination = hints.singleValueHint[Boolean](containerType, param.key, (h, k) => h.runComplexParameterRecombinationOn(k)).getOrElse(true)
		val recursiveProcessParams = if (shouldRunComplexParamRecombination && param.baseType.tpe <:< typeOf[ProcessStub[_, _]]) {
			processRecombination(param.baseType.tpe, param.clazz)
		} else Nil

		allParamsAddedInHints ::: allDefaultParamsInProcess ::: recursiveProcessParams
	}

	protected def getApplicableTypesInDB(baseType: Type) = {
		db.types.filter(dbType => dbType._1 <:< baseType)
	}
}