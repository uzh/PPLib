package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Created by pdeboer on 27/05/15.
 */
class Recombinator[INPUT, OUTPUT <: Comparable[OUTPUT]](recombinable: DeepStructure[INPUT, OUTPUT]) {
	lazy val variants = {
		val recombinations = recombinable.defineRecombinationSearchSpace.par.map {
			case (key, value) => {
				val tpeTag = value.typeTag.asInstanceOf[TypeTag[ProcessStub[_, _]]]
				val classTag = value.classTag.asInstanceOf[ClassTag[ProcessStub[_, _]]]
				key -> new TypeRecombinator(value.hints)
					.materialize[ProcessStub[_, _]]()(tpeTag, classTag)
			}
		}.toList.toMap //toList toMap is an ugly hack to get it to be non-parallel again

		new RecombinationVariantGenerator(recombinations).variants
	}

	def recombine(): List[SurfaceStructure[INPUT, OUTPUT]] = {
		variants.map(v => new SurfaceStructure(recombinable, v))
	}

	def getCostCeiling(data: Any): Int = {
		variants.map(_.createProcess().getCostCeiling(data)).sum
	}
}
