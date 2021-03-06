package ch.uzh.ifi.pdeboer.pplib.process.stdlib

import ch.uzh.ifi.pdeboer.pplib.process.entities._

/**
 * Created by pdeboer on 05/12/14.
 */
@PPLibProcess
class CollectDecideProcess(_params: Map[String, Any] = Map.empty) extends CreateProcess[Patch, Patch](_params) {

	import ch.uzh.ifi.pdeboer.pplib.process.stdlib.CollectDecideProcess._

	override def expectedParametersBeforeRun: List[ProcessParameter[_]] =
		List(COLLECT, DECIDE).asInstanceOf[List[ProcessParameter[_]]]


	override protected def run(data: Patch): Patch = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(data.hashCode() + "").getOrElse(new NoProcessMemoizer())

		logger.info("Running collect-phase for patch")
		val collection: List[Patch] = memoizer.mem("collectProcess")(COLLECT.get.create(if (FORWARD_PARAMS_TO_COLLECT.get) params else Map.empty).process(data))
		val collectionDistinct = collection.distinct
		logger.info(s"got ${collection.length} results. ${collectionDistinct.length} after pruning. Running decide")
		if (FORWARD_PATCH_TO_DECIDE_PARAMETER.get.isDefined)
			DECIDE.get.setParams(Map(
				FORWARD_PATCH_TO_DECIDE_PARAMETER.get.get.key
					-> FORWARD_PATCH_TO_DECIDE_MESSAGE.get.getMessage(data)), replace = true)


		val res = memoizer.mem(getClass.getSimpleName + "decideProcess")(DECIDE.get.create(if (FORWARD_PARAMS_TO_DECIDE.get) params else Map.empty).process(collectionDistinct))
		logger.info(s"Collect/decide for $res has finished with Patch $res")
		res
	}

	override def getCostCeiling(data: Patch) = {
		val createProcess: CreateProcess[Patch, List[Patch]] = COLLECT.get.create()
		val createOutputEstimate: List[Patch] = (1 to createProcess.dataSizeMultiplicator).map(l => data).toList
		createProcess.getCostCeiling(data) + DECIDE.get.create().getCostCeiling(createOutputEstimate)
	}

	override def optionalParameters: List[ProcessParameter[_]] = List(FORWARD_PATCH_TO_DECIDE_MESSAGE, FORWARD_PATCH_TO_DECIDE_PARAMETER, FORWARD_PARAMS_TO_COLLECT, FORWARD_PARAMS_TO_DECIDE)
}

object CollectDecideProcess {
	val FORWARD_PARAMS_TO_COLLECT = new ProcessParameter[Boolean]("forwardParamsToCollect", Some(List(true)))
	val FORWARD_PARAMS_TO_DECIDE = new ProcessParameter[Boolean]("forwardParamsToDecide", Some(List(true)))
	val COLLECT = new ProcessParameter[PassableProcessParam[CreateProcess[Patch, List[Patch]]]]("collect", None)
	val DECIDE = new ProcessParameter[PassableProcessParam[DecideProcess[List[Patch], Patch]]]("decide", None)
	val FORWARD_PATCH_TO_DECIDE_PARAMETER = new ProcessParameter[Option[ProcessParameter[String]]]("forwardPatchToDecideParameter", Some(List(None)))
	val FORWARD_PATCH_TO_DECIDE_MESSAGE = new ProcessParameter[PatchEmbeddedInString]("forwardPatchToDecideMessage", Some(List(new PatchEmbeddedInString("The original sentence was: "))))
}


@SerialVersionUID(1l)
case class PatchEmbeddedInString(before: String = "", after: String = "") extends Serializable {
	def getMessage(patch: Patch) = before + patch.value + after
}
