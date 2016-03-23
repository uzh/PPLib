package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompPortalAdapter
import ch.uzh.ifi.pdeboer.pplib.process.entities.{DefaultParameters, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.process.stdlib.{ContestWithBeatByKVotingProcess, ContestWithStatisticalReductionProcess}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
  * Created by pdeboer on 27/05/15.
  */
class Recombinator[INPUT, OUTPUT <: ResultWithCostfunction](recombinable: DeepStructure[INPUT, OUTPUT]) {
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

	def injectQueryLogger(f: (SurfaceStructure[INPUT, OUTPUT], HCompPortalAdapter) => HCompPortalAdapter) {
		recombine.foreach(s => {
			s.recombinedProcessBlueprint.stubs.values.foreach(proc => {
				proc.getParam(DefaultParameters.PORTAL_PARAMETER).foreach(portal => proc.setParams(Map(DefaultParameters.PORTAL_PARAMETER.key -> {
					val decoratorPortal = f(s, portal)
					decoratorPortal
				}), replace = true))
			})
		})
	}

	lazy val recombine: List[SurfaceStructure[INPUT, OUTPUT]] = {
		variants.map(v => new SurfaceStructure(recombinable, v))
	}

	def sneakPeek = recombine.groupBy(_.recombinedProcessBlueprint.stubs.values.head.baseType).values.map(_.minBy(s => {
		val p = s.recombinedProcessBlueprint.stubs.values.head
		val wc: Int = p.getParam(DefaultParameters.WORKER_COUNT).getOrElse(0)
		val conf: Double = p.getParam(ContestWithStatisticalReductionProcess.CONFIDENCE_PARAMETER).getOrElse(0d)
		val k: Int = p.getParam(ContestWithBeatByKVotingProcess.K).getOrElse(0)
		wc.toDouble + conf + k.toDouble
	})).toList

	def getCostCeiling(data: Any): Int = {
		variants.map(_.createProcess().getCostCeiling(data)).sum
	}
}
