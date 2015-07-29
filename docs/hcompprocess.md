#Crowd processes
In order to reuse some predefined interactions with crowd workers, they can be formulated within a Human Computation Process. 
Human computation processes can be nested due to the use of a complex ProcessParameter. 

##Example: The Collection process
We think it is best to learn about crowd processes by walking step by step through the inception of one of them: The Collection process.
A collection-process is very basic and is used to ask a question to a specific number of crowd workers. You can find the collection class, that gets used in PPLib [here](https://github.com/pdeboer/PPLib/blob/master/src/main/scala/ch/uzh/ifi/pdeboer/pplib/process/stdlib/Collection.scala)
 
###Step 1: The class definition using an INPUT, an OUTPUT and a base type
In order to make a process (re-)usable, it often pays out to think of a base class in the PPLib Process Repository (PPR) where it would fit in. 
There are currently two types of base classes present in the PPR: 
* _CREATE_: Any process where crowd workers are used to create something new
* _DECIDE_: Any process where crowd workers are asked to decide between some alternatives

In case of our Collection-Process, it would make sense to list it under `CREATE`, since we are asking a couple of them to _create_ an answer to a text.

As an input parameter, our target process will require an element about which it will ask crowd workers a question. We will therefore get multiple answers for an individual element. 
This `element` could be anything: a paragraph, a sentence, an image etc. We therefore have introduced a data structure within PPLib that takes care of capturing this in an abstract fashion: [Patch](https://github.com/pdeboer/PPLib/blob/master/src/main/scala/ch/uzh/ifi/pdeboer/pplib/process/entities/Patch.scala).
Given this explanation, our input type will be one patch, while our output type will be a list of patches (since a specific number are going to create new patches that have some relationship to the input-patch). 

These 3 decisions (input, output and base type) determine the base class out of the PPR that gets used. In case of Collection, we will use a CreateProcess with Patch as input parameter and a list of patches as output parameter: `CreateProcess[Patch, List[Patch]]`. 
The class definition of our collection process therefore looks like this (pay special attention to the 'extends..' part)
```scala
@PPLibProcess
class Collection(params: Map[String, Any] = Map.empty) extends CreateProcess[Patch, List[Patch]](params) with HCompPortalAccess with InstructionHandler with QueryInjection {
//..
}
```

In case you'd like to have your process automatically added to the PPR, add an @PPLibProcess annotation just before the class definition. Once PPLib is started, the PPR scans the classpath for all classes that have this annotation and adds them to the default PPR (you can create your own PPR with a subset of the processes). In case your process for some reason does not fulfill our default structure (see section below for more information), you can supply a ProcessFactory to this annotation using the builder pattern. Example: `@PPLibProcess(builder=classOf[MyProcessBuilder])`
We then define the class and its constructors, which should be the same for all PPLib processes. After the base type definition, we mix in Traits for all supported operations of this process: `with HCompPortalAccess with InstructionHandler with QueryInjection`. 
* _HCompPortalAccess_: Trait that introduces a parameter requiring a valid human computation portal and a convenient getter for this portal, such that within our process, we can directly interact with a variable called `portal`. 
* _InstructionHandler_: This Collection class supports instruction handling, which is employed to generate instructions according to a definition of the deep structure of the instructions. You can read more about instruction generation [here](https://github.com/pdeboer/PPLib/blob/master/docs/instructiongenerator.md). The trait introduces a method `instructions.getInstruction(..)` and `instructionTitle`, both generating text based on parameters
* _QueryInjection_: This trait supports injecting multiple additional queries into this process that get sent along to crowd workers with the actual query defined by the process programmer. As an example in the collection process, we will send a query to crowd workers based on this patch. Another person using this process, would additionally like to gain feedback by crowd workers about this task and can therefore supply this as an "injected query" that gets picked up and executed together with the actual query using this trait. Answers for injected queries can be retrieved directly on the process using `myProcess.getQueryAnswersFromComposite(..)` 

For all traits, we recommend reading the class code for more information on how to use them. 

###Step 2: Define parameters
Almost all Processes require some kind of parameters to be executed. We have provided a implementations of the most common parameters in [the DefaultParameters class](https://github.com/pdeboer/PPLib/blob/master/src/main/scala/ch/uzh/ifi/pdeboer/pplib/process/entities/DefaultParameters.scala) that you can (and should) reuse. 
 In case of the Collection class, one doesn't need any more default parameters than the ones defined by DefaultParameters: one just needs `WORKER_COUNT`. In case one would require additional parameters, that aren't present in DefaultParameters, one can add a static portion to the class into the same file as illustrated below:
 ```scala
 //don't look at this
 @PPLibProcess
 class Collection(params: Map[String, Any] = Map.empty) extends CreateProcess[Patch, List[Patch]](params) with HCompPortalAccess with InstructionHandler with QueryInjection {
 //..
 }
 
 //look at this instead
 object Collection {
 	val MYPARAMETER = new ProcessParameter[Int]("myparam", Some(List(2,3,4)))
 }
 ```

MYPARAMETER is declared as a static variable on the process itself. Please note that the actual implementation of Collection doesn't have this parameter, it's just an illustration here. 
MYPARAMETER has a type (Int), which is enforced on supplied definitions. It also specifies a list of default values that could be used in recombination. For this case, we have valid values ranging from 2-4. 

In order to make this parameter usable within the process, one needs to add it to the method of the process defining the optional parameters or to the one that defines the required parameters for a process. In our case, this parameter is definitely optional an can therefore be added to `optionalParameters()`. (Note that WORKER_COUNT is another optional parameter that is defined in DefaultParameters)
```scala
    //...
	override def optionalParameters: List[ProcessParameter[_]] = List(WORKER_COUNT, MYPARAMETER) ::: super.optionalParameters
	//...
```
Please always concatenate the list of the parameters you require with the ones passed down from super classes: Traits also write their parameters into this method. 

Now that you have associated the parameter with your process, you can - wherever you need it, call its value directly using `MYPARAMETER.get`. As you might have guessed already, parameters get passed to a process using the `Map[String,Any]` that must be present in the constructor (and should be passed to the super class as well).
You will see an example of how to create an instance of a crowd process in step 4. 
   
###Step 3: Implement the run-method. Or: Getting to work
Every process has a purpose. This purpose is implemented in the `run` method of the process. Here, we will provide a simplified implementation of the one present in the actual implementation that doesn't use QueryInjection. 
In the code below, the following steps will happen: 

1. For every crowd worker we ask the following query and put it into the `answers` variable within a List:
  1.0 We initialize the Memoizer that is responsible for inexpensive crash & rerun (If the process execution crashes, it will return from the last state that we have surrounded with an `memoizer.mem()` statement after we have fixed the problem and rerun the application)
  1.1 We construct our precise worker instructions using the _InstructionHandler_ Trait ([learn more here](https://github.com/pdeboer/PPLib/blob/master/docs/instructiongenerator.md))
  1.2 We create a query where crowd workers can answer in freetext using these instructions and store it in the `query` variable
  1.3 We send the query to our portal (mixed in by the `HCompPortalAccess` trait) and wait for it to give us a response. (Happens synchronously, but in parallel)
  1.4 We return the answer into the answers field
2. We then create new patches that are based on the supplied patch (called `line`) and add the crowd worker answers to them. These are then returned

```scala
	override protected def run(line: Patch): List[Patch] = {
		val memoizer: ProcessMemoizer = getProcessMemoizer(line.hashCode() + "").getOrElse(new NoProcessMemoizer())
		
        val answers = getCrowdWorkers(WORKER_COUNT.get).map(w => {
            memoizer.mem(s"answer_line_$line_worker_$w") {
                val instr: String = instructions.getInstructions(line + "", htmlData = QUESTION_AUX.get.getOrElse(Nil))
                val mainQuery: FreetextQuery = FreetextQuery(
                    instr, "", instructionTitle + w + "_" + Math.abs(Random.nextInt()))
                val query = createComposite(mainQuery)
                portal.sendQueryAndAwaitResult(query, QUESTION_PRICE.get).is[FreetextAnswer].answer
		    }	
        }).toList
        answers.map(a => line.duplicate(a))
	}
```


##Default structure of a process
All PPLib processes have a default constructor that looks like this: `class MyProcess(params: Map[String, Any] = Map.empty) extends ..`. 
Even though there are ways to go around it if absolutely necessary, processes should stick to the following constraints
* Have 2 constructors: one that takes a single parameter of type `Map[String,Any]` and one that takes no parameter (and forwards an empty Map into the first constructor). Note that all of these get automatically generated if you use the class header described above
* Use parameters from [DefaultParameters](https://github.com/pdeboer/PPLib/blob/master/src/main/scala/ch/uzh/ifi/pdeboer/pplib/process/entities/DefaultParameters.scala) wherever applicable (worker counts, worker payout, etc)
* In case a parameter is required and not present in DefaultParameters, the parameter should be added as a static element to the class definition for increased readability (see step 3 above for an example)
* Read _all_ parameters your process needs from the parameter map that gets passed to the class. There are convenience methods to make them more accessible and type-safe.
* Specify the `costCeiling` and `dataSizeMultiplicator` function for your process. You can find an easy example in the [Collection class](https://github.com/pdeboer/PPLib/blob/master/src/main/scala/ch/uzh/ifi/pdeboer/pplib/process/stdlib/Collection.scala)
* Try to build your class in a way such that it only requires optional parameters by giving default values to all parameters that you specify yourself