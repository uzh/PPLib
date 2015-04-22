package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, SimpleClassTag}

import scala.reflect.runtime.universe._



/**
 * Created by pdeboer on 27/03/15.
 */
class Recombinator[INPUT_TYPE, OUTPUT_TYPE](hints: Iterable[RecombinationHintGroup] = List.empty[RecombinationHintGroup], db: RecombinationDB = RecombinationDB.DEFAULT)(implicit inputClass: TypeTag[INPUT_TYPE], outputType: TypeTag[OUTPUT_TYPE]) extends LazyLogger {
	def materialize(hintsToUse: Option[Class[ProcessStub[_, _]]] = None): List[PassableProcessParam[ProcessStub[INPUT_TYPE, OUTPUT_TYPE]]] = {
		val defaultHints: List[RecombinationHint] = getHintsToUse(hintsToUse)
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

				}

				p.clazz match {
					case passable: Class[PassableProcessParam[_]] => {
						logger.info(s"  recursively recombining $p..")
						val pr = passable.newInstance().clazz.asInstanceOf[Class[ProcessStub[_, _]]]
						var hintsMap = hints.groupBy(_.classUnderRecombination)
						val currentHintsLineForThisClass = hintsMap.get(Some(pr)).map(g => g.toList.head).map(_.hints).getOrElse(List())
						val newHintsToUseForThisClass = new RecombinationHintGroup(Some(pr), new TypeUnsafeRecombinationHint(pr) :: currentHintsLineForThisClass)
						hintsMap += Some(pr) -> List(newHintsToUseForThisClass)

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


	private def getHintsToUse(baseClassFilter: Option[Class[ProcessStub[_, _]]]): List[RecombinationHint] = {
		hints.find(h => h.classUnderRecombination == baseClassFilter).map(_.hints).getOrElse(Nil)
	}
}

object MyTest extends App {

	class A

	class B extends A

	class C

	val t = typeOf[Class[A]]
	val t2 = typeOf[B]
	val t3 = typeOf[C]
	println("A is parent of B " + (t2 <:< t.typeArgs.head)) //true
	println("B is parent of A " + (t.typeArgs.head <:< t2)) //true
	println("C is related to B" + (t3 <:< t.typeArgs.head))
	//false

	val classOfA = classOf[A]
	val typeOfA = runtimeMirror(classOfA.getClassLoader).classSymbol(classOfA).toType
	println(t3 <:< typeOfA)

}