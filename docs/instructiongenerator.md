#Using the Instruction Generator
The instruction generator creates specific instructions based on the instruction-deep-structure provided through parameters. 
Note that the instruction deep structure can be specified using the `InstructionData` class. 

Please note that this is a fairly advanced topic and requires you to be proficient with [crowd processes and parameter handling in PPLib](https://github.com/uzh/PPLib/blob/master/docs/hcompprocess.md).

##Overview
A PPLib process that uses instruction generation needs to implement the trait `InstructionHandler`. 

Instruction generation varies with process type. There is a different generator at work for CREATE-type processes (default is `SimpleInstructionGeneratorCreate`) than for DECIDE-type processes (default is `SimpleInstructionGeneratorDECIDE`). 
Processes can specify their default instruction generator by overriding `processInstructionGenerator()` in their class. It is further possible for the end user to override the process' preferred instruction generator by passing a generator as a parameter called `OVERRIDE_INSTRUCTION_GENERATOR`.

This instruction generator is able to generate questions when combining it with an instance of `InstructionData` in the method `myInstructionGenerator.generateQuestion(instructionData)`. 
This method returns a `QuestionRenderer`, which is responsible for displaying the question in a format that makes sense to the target portal.
  
In short, the `InstructionHandler` trait uses the following order to determine the target instruction generator for a class:
 
1. Is there a parameter `OVERRIDE_INSTRUCTION_GENERATOR` set? If so, use that one

2. Is there an instruction generator set on the process itself using `process.processInstructionGenerator`? If so, use that one

3. If nothing is specified, return the default instruction generator. 
 
##Example
Below you'll find an example for a class that uses instruction generation. 

```scala
//...
	override protected def run(line: Patch): List[Patch] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(line.hashCode() + "").getOrElse(new NoProcessMemoizer())
		
        val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {
            memoizer.mem(s"answer_line_$line_worker_$w") {
                val instr: String = instructions.getInstructions(line + "", htmlData = QUESTION_AUX.get.getOrElse(Nil))
                val mainQuery: FreetextQuery = FreetextQuery(
                    instr, "", instructionTitle + w + "_" + Math.abs(Random.nextInt()))
                portal.sendQueryAndAwaitResult(mainQuery, QUESTION_PRICE.get).is[FreetextAnswer].answer
		    }	
        }).toList
        answers.map(a => line.duplicate(a))
	}
//..
```

1. `instructions` is a method part of the `InstructionHandler`-trait. It returns a question renderer following the workflow explained above.
 
2. `getInstructions` is part of the QuestionRenderer and formats the question for the target portal by embedding it into some HTML code. 
 
3. `instructionTitle` is yet another method part of the `InstructionHandler`-trait. It employs the target instruction generator to generate the title. 
