package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Created by pdeboer on 27/05/15.
 */
class Recombinator(recombinable: Recombinable[_]) {
	def recombine() = {
		val recombinations = recombinable.requiredProcessDefinitions.map {
			case (key, value) => {
				val tpeTag = value.typeTag.asInstanceOf[TypeTag[ProcessStub[_, _]]]
				val classTag = value.classTag.asInstanceOf[ClassTag[ProcessStub[_, _]]]
				key -> new TypeRecombinator(value.hints)
					.materialize[ProcessStub[_, _]]()(tpeTag, classTag)
			}
		}.toMap

		new RecombinationVariantGenerator(recombinations).variants
	}
}
