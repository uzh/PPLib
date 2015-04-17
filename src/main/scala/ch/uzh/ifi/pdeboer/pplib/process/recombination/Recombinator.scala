package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, SimpleClassTag}

import scala.reflect.ClassTag

/**
 * Created by pdeboer on 27/03/15.
 */
class Recombinator[INPUT_TYPE, OUTPUT_TYPE](hints: Iterable[RecombinationHintGroup] = List.empty[RecombinationHintGroup], db: RecombinationDB = RecombinationDB.DEFAULT)(implicit inputType: ClassTag[INPUT_TYPE], outputType: ClassTag[OUTPUT_TYPE]) extends LazyLogger {
	def materialize(hintsToUse: Option[Class[ProcessStub[_, _]]] = None): List[PassableProcessParam[ProcessStub[INPUT_TYPE, OUTPUT_TYPE]]] = {
		val defaultHints: List[RecombinationHint] = getHintsToUse(hintsToUse)
		val targetClasses = db.classes.filter(t => defaultHints.forall(c => c.filter(t)))
		targetClasses.map(cls => {
			val gen = new TypedParameterVariantGenerator[ProcessStub[INPUT_TYPE, OUTPUT_TYPE]]()(new SimpleClassTag[INPUT_TYPE, OUTPUT_TYPE](cls))
			defaultHints.foreach(hint => {
				hint.processConstructionParameter.foreach {
					case (parameterKey, parameterValue) => gen.addParameterVariations(parameterKey, parameterValue)
				}
			})
			gen.uncoveredParameterThatAreExpected.foreach(p => {
				if (p.clazz.isInstanceOf[PassableProcessParam[_]]) {
					logger.info(s"recursively recombining $p..")
					val pr = p.clazz.asInstanceOf[Class[ProcessStub[_, _]]]
					var hintsMap = hints.groupBy(_.classUnderRecombination)
					val currentHintsLineForThisClass = hintsMap.get(Some(pr)).map(g => g.toList.head).map(_.hints).getOrElse(List())
					val newHintsToUseForThisClass = new RecombinationHintGroup(Some(pr), new TypeUnsafeRecombinationHint(pr) :: currentHintsLineForThisClass)
					hintsMap += Some(pr) -> List(newHintsToUseForThisClass)

					gen.addParameterVariations(p.key, new Recombinator[Any, Any](hintsMap.values.flatten, db).materialize(Some(pr)))
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