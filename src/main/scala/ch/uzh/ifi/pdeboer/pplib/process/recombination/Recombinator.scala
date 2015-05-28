package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PassableProcessParam, ProcessStub}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Created by pdeboer on 27/05/15.
 */
class Recombinator(recombinable: Recombinable[_]) {
	def recombine() = {
		val recombinations: Map[String, List[PassableProcessParam[ProcessStub[_, _]]]] = recombinable.requiredProcessDefinitions.par.map {
			case (key, value) => {
				val tpeTag = value.typeTag.asInstanceOf[TypeTag[ProcessStub[_, _]]]
				val classTag = value.classTag.asInstanceOf[ClassTag[ProcessStub[_, _]]]
				key -> new TypeRecombinator(value.hints)
					.materialize[ProcessStub[_, _]]()(tpeTag, classTag)
			}
		}.toList.toMap //toList toMap is an ugly hack to get it to be non-parallel again

		new RecombinationVariantGenerator(recombinations).variants
	}
}
