package ch.uzh.ifi.pdeboer.pplib.examples.recombination.translation

import ch.uzh.ifi.pdeboer.pplib.examples.recombination.translation.TranslationProcess._
import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompInstructionsWithTuple
import ch.uzh.ifi.pdeboer.pplib.patterns.{FFVFindQuestion, FFVFixQuestion, FFVFixQuestionInclOtherPatches, FFVPatch}
import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.FindFixVerifyProcess._
import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib._
import ch.uzh.ifi.pdeboer.pplib.recombination.{ProcessStub, RecombinationVariantGenerator, TypedParameterVariantGenerator}

/**
 * Created by pdeboer on 28/11/14.
 */
object TranslationRecombination {
	def recombinations = {
		val candidateProcessParameters = Map(
			REWRITE -> List(
				new TypedParameterVariantGenerator[FindFixVerifyProcess]()
				/*
					new TypedParameterVariantGenerator[DualPathwayProcess]()
						.addVariation(DualPathwayProcess.QUESTION_NEW_PROCESSED_ELEMENT, List(HCompInstructionsWithTuple("Please fix any problems you see in the sentence below")))
						.addVariation(DualPathwayProcess.QUESTION_OLD_PROCESSED_ELEMENT, List(HCompInstructionsWithTuple("Please check if the sentence below is fixed correctly. If it's not, please fix it yourself", questionBetweenTuples = "Candidate answer"))),
					new TypedParameterVariantGenerator[IterativeRefinementProcess](),

				new TypedParameterVariantGenerator[ContestWithSigmaPruning]()
	*/
			),
			IMPROVE_LANGUAGE_QUALITY -> List(
				new TypedParameterVariantGenerator[FindFixVerifyProcess]()
					.addVariation(FIND_QUESTION, List(new FFVFindQuestion("You'll find a couple of paragraphs below in the order where they would occur in a text. Please select items where paragraph transitions are not optimal, paragraphs that lack consistent wording or whose wording could be simplified.")))
					.addVariation(FIX_QUESTION, List(new FFVFixQuestionInclOtherPatches("Other crowd workers have selected the paragraph below because its paragraph transitions were not optimal or because its wording was inconsistent.", questionAfterPatch = "Please fix the paragraph such that it fits into the following context. Your submission will be evaluated according to it's simplicity (make the text as simple as possible without losing information), paragraph transitions and on whether it fits with the wording of the other paragraphs", allDataDisplayFunction = f => f.mkString("<br/><br/>"))))
					.addVariation(VERIFY_PROCESS, List(new SelectBestAlternativeWithFixWorkerCount(Map(
					SelectBestAlternativeWithFixWorkerCount.INSTRUCTIONS_PARAMETER.key ->
						HCompInstructionsWithTuple("Other crowd workers have come up with the following alternatives for the paragraph below. Please select the one you think works best considering paragraph transitions, consistency of wording and simplicity of the sentence"))
				)))
					.addVariation(VERIFY_PROCESS_CONTEXT_PARAMETER, List(Some(SelectBestAlternativeWithFixWorkerCount.AUX_STRING_PARAMETER)))
					.addVariation(VERIFY_PROCESS_CONTEXT_FLATTENER, List((l: List[FFVPatch[String]]) => l.mkString("\\n")))
			),
			CHECK_SYNTAX -> List(
				new TypedParameterVariantGenerator[FindFixVerifyProcess]()
					.addVariation(FIND_QUESTION, List(new FFVFindQuestion("Please select paragraphs with syntactical errors in the list below. Syntactical errors include spelling mistakes, interpunctuation (commas/points wrong).")))
					.addVariation(FIX_QUESTION, List(new FFVFixQuestion("Other crowd workers have identified the paragraphs below to include syntactical mistakes (wrong commas/points, spelling mistakes etc). Please fix all mistakes in the paragraph")))
					.addVariation(VERIFY_PROCESS, List(new SelectBestAlternativeWithFixWorkerCount(Map(
					SelectBestAlternativeWithFixWorkerCount.INSTRUCTIONS_PARAMETER.key ->
						HCompInstructionsWithTuple("Other crowd workers have come up with the following alternatives for the paragraph below. Please select the one that works best considering interpuctuation (commas, points) and spelling mistakes"))
				)))
					.addVariation(VERIFY_PROCESS_CONTEXT_PARAMETER, List(Some(SelectBestAlternativeWithFixWorkerCount.AUX_STRING_PARAMETER)))
					.addVariation(VERIFY_PROCESS_CONTEXT_FLATTENER, List((l: List[FFVPatch[String]]) => l.mkString("\\n")))
			)
		)

		val candidateProcesses = candidateProcessParameters.map {
			case (key, generators) => (key, generators.map(_.generateVariationsAndInstanciate()).flatten.asInstanceOf[List[ProcessStub[_, _]]])
		}

		new RecombinationVariantGenerator(candidateProcesses).variants
	}
}
