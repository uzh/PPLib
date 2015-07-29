# PPLib
PPLib People Programming LIBrary: Easily find the best Crowdsourcing process for your application

# Setup
Before setting up PPLib, please install [Java 8](https://www.java.com/en/download/help/download_options.xml), [sbt](http://www.scala-sbt.org/release/tutorial/Setup.html) and [git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) on your system. 
1. Download PPLib onto your machine. In Terminal, type `git clone https://github.com/pdeboer/PPLib`

2. Install PPLib on your system. In Terminal: `cd PPLib && sbt publish-local`

3. In the `build.sbt` file of your SBT-enabled application, add the PPLib dependency: `libraryDependencies += "pdeboer" %% "pplib" % "0.1-SNAPSHOT"`. You can find an example `build.sbt` file (with other dependencies as well) [here](https://github.com/pdeboer/PPLibBallotConnector/blob/master/build.sbt)

4. If you don't have one yet, add an `application.conf` file in your target application in the `resources` folder and configure your access code to the human computation portals you'd like to use. An example configuration file can be found [here](https://github.com/pdeboer/PPLib/blob/master/src/main/resources/application.conf_default)

# How-to
## Examples
### Simple example on how to interact with Portals (MTurk / CrowdFlower)
Please find a very simple example that doesn't use Recombination or Crowd Processes at all [here](https://github.com/pdeboer/PPLib/blob/master/src/test/scala/ch/uzh/ifi/pdeboer/pplib/examples/Survey.scala). 
This example shows you how to ask crowd workers questions and how to act upon their answers. 

### A Simple Crowd Process 
The `Collection process` is very simple: It just asks a specific number of people the same question. You can find the collection class, that gets used in PPLib [here](https://github.com/pdeboer/PPLib/blob/master/src/main/scala/ch/uzh/ifi/pdeboer/pplib/process/stdlib/Collection.scala)

### Recombination
Recombination is used to generate all possible implementations of a crowd task as specified by its deep structure.
You can find a full Recombination example [here](https://github.com/pdeboer/PPLib/tree/master/src/test/scala/ch/uzh/ifi/pdeboer/pplib/examples/textshortening). Take a look at the callable Main class `ShortNText` first to see how everything fits together.  
 
## Guides
* [Crowd Processes](https://github.com/pdeboer/PPLib/blob/master/docs/hcompprocess.md)
* [How to run the Recombinator](https://github.com/pdeboer/PPLib/blob/master/docs/recombination.md)
* [How to create a new HComp Portal](https://github.com/pdeboer/PPLib/blob/master/docs/hcompportal.md)
* [How to use question generation](https://github.com/pdeboer/PPLib/blob/master/docs/instructiongenerator.md)

# Contact
Write [Patrick](pdeboer@ifi.uzh.ch)