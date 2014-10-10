package ch.uzh.ifi.pdeboer.crowdlang.recombination

/**
 * Created by pdeboer on 09/10/14.
 */
class RecombinationStub[INPUT, PROCESS_INPUT, PROCESS_OUTPUT, OUTPUT](
																		 inputPreprocessor: INPUT => PROCESS_INPUT, processor: PROCESS_INPUT => PROCESS_OUTPUT,
																		 outputPostprocessor: PROCESS_OUTPUT => OUTPUT) {
	def run(data: INPUT): OUTPUT =
		outputPostprocessor(processor(inputPreprocessor(data)))
}

class SimpleRecombinationStub[PROCESS_INPUT, PROCESS_OUTPUT](processor: PROCESS_INPUT => PROCESS_OUTPUT) extends RecombinationStub[PROCESS_INPUT, PROCESS_INPUT,
	PROCESS_OUTPUT, PROCESS_OUTPUT](i => i, processor, o => o)
