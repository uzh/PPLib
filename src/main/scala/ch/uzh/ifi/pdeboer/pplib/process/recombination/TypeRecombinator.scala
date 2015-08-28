package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessParameter, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._


/**
 * Created by pdeboer on 27/03/15.
 */
class TypeRecombinator(hints: RecombinationHints, db: RecombinationDB = RecombinationDB.DEFAULT) extends LazyLogger {
	def materialize[BASE <: ProcessStub[_, _]]()(implicit baseType: TypeTag[BASE], baseClass: ClassTag[BASE]): List[PassableProcessParam[BASE]] = {
		val result: List[PassableProcessParam[BASE]] = processRecombination(baseType.tpe, baseClass.runtimeClass.asInstanceOf[Class[BASE]]).asInstanceOf[List[PassableProcessParam[BASE]]]
		logger.info(s"generated ${result.size} candidates for base type $baseType matching $hints")
		result
	}

	def processRecombination(baseType: Type, baseClass: Class[_ <: ProcessStub[_, _]]): List[PassableProcessParam[ProcessStub[_, _]]] = {
		val applicableTypes = getApplicableTypesInDB(baseType)
		val recombinationsOfTheseTypes = applicableTypes.flatMap { case (processType, processClass) => {
			//TODO check for circles
			val baseProcess: ProcessStub[_, _] = ProcessStub.createFromBlueprint(processClass, Map.empty)

			val variantGenerator = new InstanciatedParameterVariantGenerator[ProcessStub[_, _]](baseProcess)
			baseProcess.allParams.foreach(p =>
				variantGenerator.addVariation(p, generateCandidatesForParameter(baseProcess, processType, p))
			)
			variantGenerator.generatePassableProcesses()
		}
		}

		recombinationsOfTheseTypes
	}

	protected def generateCandidatesForParameter[T <: ProcessStub[_, _]](processUnderRecombination: T, containerType: Type, param: ProcessParameter[_]): List[Any] = {
		val allParamsAddedInHints = hints(processUnderRecombination.getClass).flatMap(_.processConstructionParameter.getOrElse(param.key, Nil))
		val isAllowedToAddGeneralDefaultParams = hints.singleValueHint[Boolean](containerType, param.key, (h, k) => h.addDefaultValuesFromParamDefinition(k)).getOrElse(true)
		val allDefaultGeneralParamsInProcess = if (isAllowedToAddGeneralDefaultParams) param.candidateDefinitions.getOrElse(Nil).toList else Nil

		val isAllowedToAddLocalDefaultParams = hints.singleValueHint[Boolean](containerType, param.key, (h, k) => h.addDefaultValuesFromProcessDefinition(k)).getOrElse(true)
		val allDefaultLocalParamsInProcess = if (isAllowedToAddLocalDefaultParams) processUnderRecombination.processParameterDefaults.getOrElse(param, Nil) else Nil

		val shouldRunComplexParamRecombination = hints.singleValueHint[Boolean](containerType, param.key, (h, k) => h.runComplexParameterRecombinationOn(k))
			.getOrElse(true)
		val recursiveProcessParams = if (shouldRunComplexParamRecombination && param.baseType.tpe <:< typeOf[PassableProcessParam[_]]) {
			//process-parameters are encapsulated within PassableProcessParam --> need to extract type from generics argument
			val encapsulatedType: Type = param.baseType.tpe.typeArgs.head
			val encapsulatedClass: ClassSymbol = encapsulatedType.baseClasses.head.asClass
			val clazz: RuntimeClass = runtimeMirror(encapsulatedClass.getClass.getClassLoader).runtimeClass(encapsulatedClass)
			processRecombination(encapsulatedType, clazz.asInstanceOf[Class[_ <: ProcessStub[_, _]]])
		} else Nil

		allParamsAddedInHints ::: allDefaultGeneralParamsInProcess ::: allDefaultLocalParamsInProcess ::: recursiveProcessParams
	}

	def getApplicableTypesInDB(baseType: Type) = {
		val directSpecializations = db.types.filter(dbType => dbType._1 <:< baseType)
		val specializationsInGenericsHierarchy = Nil

		directSpecializations ::: specializationsInGenericsHierarchy
	}
}