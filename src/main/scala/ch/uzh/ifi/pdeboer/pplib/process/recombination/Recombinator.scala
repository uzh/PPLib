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

/*
def materialize(base: Option[Class[ProcessStub[_, _]]] = None): List[PassableProcessParam[ProcessStub[INPUT_TYPE, OUTPUT_TYPE]]] = {
		val targetClasses = db.classes.filter(t => defaultHints.forall(c => c.filter(t)))
		targetClasses.map(cls => {
			logger.info(s"found target class $cls")
			val gen = new TypedParameterVariantGenerator[ProcessStub[INPUT_TYPE, OUTPUT_TYPE]]()(new SimpleClassTag[INPUT_TYPE, OUTPUT_TYPE](cls), null)
			defaultHints.foreach(hint => {
				hint.processConstructionParameter.foreach {
					case (parameterKey, parameterValue) => gen.addParameterVariations(parameterKey, parameterValue)
				}
			})

			gen.uncoveredParameterThatAreExpected.foreach(p => {
				if (p.baseType.tpe <:< typeTag[ProcessStub[_, _]].tpe) {
					//Process recombination
					val applicableTypes: List[Class[_ <: ProcessStub[_, _]]] = db.types.filter(dbType => dbType._1 <:< p.baseType.tpe).map(_._2)
					//val applicableProcesses = applicableTypes.map(t =>  )
				}

				p.clazz match {
					case passable: Class[PassableProcessParam[_]] => {
						logger.info(s"  recursively recombining $p..")
						val pr = passable.newInstance().clazz.asInstanceOf[Class[ProcessStub[_, _]]]
						var hintsMap = hints.groupBy(_.classUnderRecombination)
						/*val currentHintsLineForThisClass = hintsMap.get(Some(pr)).map(g => g.toList.head).map(_.hints).getOrElse(List())
						val newHintsToUseForThisClass = new RecombinationHintGroup(Some(pr), new TypeUnsafeRecombinationHint(pr) :: currentHintsLineForThisClass)
						hintsMap += Some(pr) -> List(newHintsToUseForThisClass)
*/
						val recombinationsOfThisProcess = new Recombinator[Any, Any](hintsMap.values.flatten, db).materialize(Some(pr))
						logger.info(s" recursively recombined $p. found ${recombinationsOfThisProcess.size} recombinations")
						gen.addParameterVariations(p.key, recombinationsOfThisProcess)
					}
					case _ => {}
				}

				//int, String etc
				//TODO find all applicable classes using some strategy/resolver and add it as passable
			})

			gen.generatePassableProcesses()
		}

		).toList.flatten
	}
 */