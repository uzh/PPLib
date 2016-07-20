#Auto-Execution
PPLib currently supports 2 strategies to evaluate the performance of crowd processes: NAE (Naive Auto-Experimentation) and BOA (Bayesian Optimized Auto-Exerimentation). 
 Both work by taking a set of (usually [generated](recombination.md)) candidate crowd processes and running a subset of their candidates directly on a (paid) crowd market.
   The employed strategy (BOA or NAE) decides which candidates are sampled this way, how many times each candidate gets sampled and how to identify the _best_ process among the set of candidates using a utility function. 
   This _utility function_ needs to be specified on the the Deep Structure 

##Usage example
In order to use any Auto-Execution Strategy, you need to define the problem you are trying to solve within a process deep structure. 
This is the most abstract representation of the problem you are trying to solve. 
For example, if we want to answer a single question, this could be modelled through a *DECIDE*-type operator (more in this in [the recombination chapter](recombination.md)). Have a look at [this source file](https://github.com/uzh/PPLib/blob/master/src/test/scala/ch/uzh/ifi/pdeboer/pplib/examples/textshortening/ShortNDeepStructure.scala) for a full example. 

Besides the need to specify the problem (through such a deep structure), one also needs to define a utility function µ. This is done through 2 steps: 


_Algorithm 1. First step: define the surface structure's result object_
```scala
class ShortNDeepStructure extends SimpleDeepStructure[String, ShortNResult] {
	override def run(data: String, blueprint: RecombinedProcessBlueprint): ShortNResult = {
		//some deep structure here
		return myShortnResult
	}
}
```
In the very first line of the example above, one sees that this deep structure (ShortNDeepStructure) extends SimpleDeepStructure. It takes a *String* and returns a result object *ShortNResult*. The ShortNResult is a POJO, containing all data your crowd process should return. For example, if you want to find a crowd process to shorten text, you would probably want your crowd process to return the shortened text, the total cost of the process and the total duration of the crowd process. 

Next, the utility function µ needs to be defined directly on the result object.

_Algorithm 2. Second step: define the utility function on the result object_
```scala
case class ShortNResult(text: String, costInCents: Int, durationInSeconds: Int) extends ResultWithCostfunction {

	override def costFunctionResult: Double = -1 * text.length * costInCents * durationInSeconds
}
```
In PPLib all utility functions (to be maximized) are by default specified as cost functions to be minimized. The cost function -µ in this case is calculated by multiplying the text length, the cost and the duration, since bigger values in all variables lead to worse results. 

Using this deep structure, we can now run Auto-Experimentation on the recombined results. Example code for text shortening is given by [this file](https://github.com/uzh/PPLib/blob/master/src/test/scala/ch/uzh/ifi/pdeboer/pplib/examples/textshortening/ShortNText.scala). The general approach to eliciting the best process in a list according to our utility function can now be executed the following lines of code: 
```scala
val recombinations = new Recombinator(deepStructure).recombine //contains all candidate crowd processes

val autoExperimentation = new NaiveAutoExperimentationEngine(recombinations) //prepares NAE to find the best candidate among the recombinations
val results = autoExperimentation.run("my text to be shortened. blabla. shorten me") //runs one iteration of NAE on the supplied text. 

println(s"best result: ${results.bestProcess}")

```
At the end, the best process can be queried through `results.bestProcess`. As mentioned above, there are currently 2 strategies one can use for AutoExperimentation. In this example, we used NAE (NaiveAutoExperimentationEngine). The alternative is the more cost-efficient BOA (Bayesian-Optimized Auto-Experimentation). Both will be elaborated in more detail below. 


##NAE (Naive Auto Experimentation)
Given a set of candidate processes, NAE just runs all of them n-times (default=4). It passes their results into the user-defined utility function µ and returns the process with the highest average result of µ. NAE is therefore rather expensive, but is guaranteed to find the best result (as limited by the amount of repetitions.). BOA, on the other hand, approximates the best result only. 

##BOA (Bayesian Optimized Auto-Experimentation)
BOA is much more cost-efficient than NAE. It requires some configuration, but is also very simple to use. BOA only approximates NAE's results, i.e. it doesn't always nominate the process that would yield the highest utility - but it generally finds very good processes (see our paper). 

An example file using BOA is [the simulation example](https://github.com/uzh/PPLib/blob/master/src/main/scala/ch/uzh/ifi/pdeboer/pplib/examples/optimizationSimulation/MCOptimizationSimulation.scala). The key difference to the discussed example using NAE above, is the line with the definition of the Auto-Execution Engine:

```scala
val recombinations = new Recombinator(deepStructure).recombine //contains all candidate crowd processes

val autoExperimenter = new BOAutoExperimentationEngine(recombinations, new File("/Users/pdeboer/Spearmint"), "testBO")
val results = autoExperimentation.run("my text to be shortened. blabla. shorten me") //runs one iteration of NAE on the supplied text. 

println(s"best result: ${results.bestProcess}")
```
We now use the class `BOAutoExperimentationEngine`. This class expects 3 mandatory parameters: 
* the set of candidate crowd processes to operate on (`recombinations` in the example above)
* the path to Spearmint, the implementation of our Bayesian Optimizer
* the name of the experiment to use (needs to be unique)

We have posted some changes to the amazing _Spearmint_ optimizer by Jasper Snoek et al. You can find our changes on [Patrick's GitHub Account](https://github.com/pdeboer/spearmint). Clone this repository somewhere on your computer (where you can easily find it afterwards) using `git clone https://github.com/pdeboer/spearmint`. Note, that Spearmint only works with Python 2.7 (<3.x). You can configure the Python executable you'd like to use in the constructor of `BOAutoExperimentationEngine`,

If you cancel BOA during its execution, you can just restart it later and it will resume evaluating the last process it was looking at. 