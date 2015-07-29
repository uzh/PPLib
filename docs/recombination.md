#Recombination
Recombination is used to generate all possible implementations of a crowd task as specified by its deep structure. 

##Example
You can find a full Recombination example [here](https://github.com/pdeboer/PPLib/tree/master/src/test/scala/ch/uzh/ifi/pdeboer/pplib/examples/textshortening). This guide is merely to focus your attention on some specifics. 

Please note, that this section is based on the original PPLib paper. We try our best to keep it up to date as the code evolves. 
 
As an example use case, we are interested in shortening a book with 1000 pages. Our first step is to take a small sample of the book and use that sample to find the best crowd process to apply to the remainder of book. 

###STEP 1 – Intelligence: Identifying the process deep structure. 
After taking a sample of the book, we need to define the deep structure for the shortening process as well as a utility function. Algorithm 1 shows a possible implementation in Scala-code. 

The deep structure captures the core actions of text shortening: (i) dividing the sample into its paragraphs, (ii) obtaining a crowd process to shorten the paragraphs, (iii) running this process and then (iv) returning the result. 

_Algorithm 1. Process deep structure for text shortening_

```scala
case class ShortNResult(text:String, costInCents:Int, durationInSeconds:Int) extends 
	Comparable[ShortNResult] {

	override def compareTo(o:ShortNResult) = [..] // put utility function here
}

// The deep structure needs to implement PPLib’s DeepStructure interface. SimpleDeepStructure 
// can be used for deep structures that only require 1 recombined crowd process. 
// The DeepStructure interface requires a definition for the input type applied to the deep 
// structure, as well as the output type. In this case, the deep structure uses a string
// input (text) and returns a ShortNResult as output (see above, captures resulting text, cost
// and execution duration) 
class ShortNDeepStructure extends SimpleDeepStructure[String, ShortNResult] {

	override def run(input:String, b:RecombinedProcessBlueprints) : ShortNResult = {
		// (i) Split the text(“data”) that was passed to this method into its paragraphs 
		// (i.e., a list of patches, as required by inputType in obtainInstanceOfCrowdProcess)
		val paragraphs = IndexedPatch.from(input)

		// (ii) b will contain the blueprint for the recombined crowd process that we’ll apply 
		// for text shortening. The first step is to create an instance of that process 
		val shortenerProcess = obtainInstanceOfCrowdProcess(b) 
		
		// (iii) supply the list of paragraphs to the process and let the crowd shorten it
		val shortenedParagraphs = shortenerProcess.process(paragraphs)

		// (iv) return result (shortened Text) with its cost and duration 
		ShortNResult(shortenedParagraphs, 
				 shortenerProcess.costSoFar,
				 shortenerProcess.durationSoFar)
	}

	def obtainInstanceOfCrowdProcess(b: RecombinedProcessBlueprints) = { 	
		//we’d like to pass the list of paragraphs to this process and get a shortened list 
		//paragraphs in return. Therefore the desired input and output-types for this process
		//are the same and equal a list of patches (which is the type of our paragraphs-
		//member variable)
		type inputType = List[Patch] 
		type outputType = inputType

		//create an instance of the process according to its deep structure and supply the
		//desired types. Return this instance
		b.createProcess[inputType, outputType]()
	}
	//[..] search space definition from ALGORITHM 2 here
}
```

The run-method receives the recombined processes as well as the text to be shortened as parameters and uses them to specify the process deep structure. (i) Since most processes in the PPR operate on a built-in data structure called Patch, the first step of the deep structure is to create patches from the supplied text sample. (ii) The deep structure then creates an instance of the crowd process it will use and (iii) executes it, by calling the process-method and passing the prepared data structure with the list of patches resembling the paragraphs.  (iv) Lastly, the result structure (ShortNResult) is constructed using the cost and duration of the the process as well as the outcome of the process, i.e. the shortened paragraphs. 

###STEP 2 – Design: Defining parameter candidates. 

After defining the deep structure, we now need to instruct the Recombinator correctly, such that it can generate surface structures that apply to our use case of shortening paragraphs using crowds as shown in Algorithm 2. (i) In a first step, we define the applicable basic type from the PPR (CREATE / DECIDE) and the input and output types. (ii) We then define the crowd worker instructions that are to be used in the target process and (iii) bind these instructions to all generated processes. (iv) Our last step is to put everything together in a search space definition and return it. 
Note that this all takes place in the same class as the one that we’ve used in step 1.

_Algorithm 2. Defining the search space for the Recombinator_
```scala
import ch.uzh.ifi.pdeboer.pplib.process.recombination.RecombinationHints._

class ShortNDeepStructure extends SimpleDeepStructure[String, ShortNResult] {
	// ... run method as shown in algorithm 1
 	override def defineSimpleRecombinationSearchSpace = {
		// (i) Our target process should be a Create-type process that takes any descendant 
		// of a list of patches as input (_ <: List[Patch]) and output. This includes 
		// specializations of lists and/or the Patch-class.
		type targetProcessType = CreateProcess[_ <: List[Patch], _ <: List[Patch]]

		// (ii) The most important restriction on the search space is to define the crowd
		// task. By using “InstructionData” we also specify how a question gets rendered
		// to the crowd. On a process, an InstructionGenerator (IG) will then phrase 
		// instructions in a way that fits the process. For example, in a majority vote 
		// process, the IG will phrase a question out of this base data to pick the best
		// shortened paragraph while paying attention to grammar and text-length.  
		val instructions = RecombinationHints.instructions(List(
			new InstructionData(actionName = "shorten the following paragraph", 
				detailedDescription = "grammar (e.g. tenses), text-length")
		))

		// (iii) Specify that we’d like to use the default hints for all building blocks that
		// the Recombinator processes. It would be possible to target specific building 
		// blocks and supply different hints to them. 
		// The default hints are just the instructions specified above.
		val hints = RecombinationHints.create(Map(DEFAULT_HINTS -> instructions))

		// (iv) Construct the search space and return it 
		RecombinationSearchSpaceDefinition[targetProcessType](hints)
	}
}
```

(i) We first define the target type from the PPR that we’d like to use (CREATE). This Create Process should be able to work on a list of patches as input as well as a list of patches as output. 
(ii) We then define the instructions that are used to generate questions to crowd workers. The automatic generation of instructions based on provided core formulations is a powerful tool within PPLib, that’s documented in more detail on GitHub, but out of scope for this publication. Here, we simply would like to note that PPLib can generate instructions automatically and adapt them to the kind of process that uses the instructions (CREATE / DECIDE). It is furthermore possible to extend PPLib with other instruction generators. 
(iii) After specifying the formula to generate instructions for crowd workers, we use this formula as our only default parameter and (iv) construct the search space using instructions and our target type definition. 

###STEP 3 – Choice: Recombination and Auto-Experimentation. 
This is the step that brings it all together: In the executable Main class of our PPLib project, (i) we first create an instance of the deep structure as specified in step 1. (ii) The deep structure is then passed to the Recombinator who uses it to generate surface structures of our process, i.e. concrete implementations. (iii) Lastly, the surface structures are supplied to the auto experimentation engine and executed there. The auto experimentation engine uses the utility function as defined in step 1 to determine the best performing process among all the surface structures.
 
_Algorithm 3.  Recombination and Auto-Experimentation_
```scala
object ShortnText extends App {
 	val text = "shorten me [..]" //text that needs to be shortened. E.g. could be loaded from PDF 

	val deepStructure = new ShortNDeepStructure //(i) instantiate step 1 & 2 from above

	//(ii) use the search space defined in the deep structure to generate the surface structures 
	val surfaceStructures = new Recombinator(deepStructure).recombine()

	//(iii) create AutoExperimentationEngine and let it run one iteration on all surface structures.
	//Note, that one can easily filter the surface structures before running auto experimentation.
	val results = new AutoExperimentationEngine(surfaceStructures).runOneIteration(text)
	
	//use utility function to find the process with the best result. Alternatively, one can also 
	//export the result and pick the best one using his optimization tool of choice. 
	val bestProcess = results.bestProcess

	println(s"The best process is $bestProcess")
}
```