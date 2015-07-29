# PPLib
PPLib People Programming LIBrary: Easily find the best Crowdsourcing process for your application

# Setup
Before setting up PPLib, please install [Java 8](https://www.java.com/en/download/help/download_options.xml), [sbt](http://www.scala-sbt.org/release/tutorial/Setup.html) and [git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) on your system.
 
1. Download PPLib onto your machine. In Terminal, type `git clone https://github.com/pdeboer/PPLib`

2. Install PPLib on your system. In Terminal: `cd PPLib && sbt publish-local`

3. In the `build.sbt` file of your SBT-enabled application, add the PPLib dependency: `libraryDependencies += "pdeboer" %% "pplib" % "0.1-SNAPSHOT"`. You can find an example `build.sbt` file (with other dependencies as well) [here](https://github.com/pdeboer/PPLibBallotConnector/blob/master/build.sbt)

4. If you don't have one yet, add an `application.conf` file in your target application in the `resources` folder and configure your access code to the human computation portals you'd like to use. An example configuration file can be found [here](https://github.com/pdeboer/PPLib/blob/master/src/main/resources/application.conf_default)

# How-to
In order to get familiar with PPLib, we recommend the following steps:

1. Read how to formulate simple questions to crowd workers and act upon their responses by looking at [this simple code](https://github.com/pdeboer/PPLib/blob/master/src/test/scala/ch/uzh/ifi/pdeboer/pplib/examples/Survey.scala). 

2. Learn how to aggregate simple question-answer routines into processes by following the narrated guide on [Crowd Processes](https://github.com/pdeboer/PPLib/blob/master/docs/hcompprocess.md). This will also teach you how the parameter system works and how to create instances of existing processes for your application

3. Learn how to use Recombination to find the best solution to your problem by following this guide: [How to run the Recombinator](https://github.com/pdeboer/PPLib/blob/master/docs/recombination.md)

4. Learn how to use HComp portals by following this guide: [How to create a new HComp Portal](https://github.com/pdeboer/PPLib/blob/master/docs/hcompportal.md)

5. Learn how to use instruction generation by following this guide: [How to use question generation](https://github.com/pdeboer/PPLib/blob/master/docs/instructiongenerator.md)

# Contact
Write [Patrick](pdeboer@ifi.uzh.ch)