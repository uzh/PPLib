package ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation

import java.io.{File, FileWriter}

import ch.uzh.ifi.pdeboer.pplib.examples.optimizationSimulation.SpearmintConfigExporter
import ch.uzh.ifi.pdeboer.pplib.process.entities.{SurfaceStructureFeatureExpander, XMLFeatureExpander}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{ResultWithUtility, SurfaceStructure}

/**
  * Created by pdeboer on 16/03/16.
  */
class BOAutoExperimentationEngine[INPUT, OUTPUT <: ResultWithUtility](surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]],
																	  pathToSpearmint: File, experimentName: String, pathToSpearmintExperimentFolder: Option[File] = None, overridePPLibPath: Option[File], sbtCommand: String = "sbt", pythonCommand: String = "python") extends AutoExperimentationEngine[INPUT, OUTPUT](surfaceStructures) {
	assert(experimentName != null && experimentName.length > 0)
	assert(pathToSpearmint.exists(), "Spearmint not found")

	protected val expander = new SurfaceStructureFeatureExpander(surfaceStructures)
	val targetFeatures = expander.featuresInclClass.filter(f => List("TypeTag[Int]", "TypeTag[Double]", XMLFeatureExpander.baseClassFeature.typeName).contains(f.typeName)).toList

	def experimentPath = {
		val dir = pathToSpearmintExperimentFolder.getOrElse(
			new File(s"${pathToSpearmint.getAbsolutePath}${File.separator}examples${File.separator}$experimentName${File.separator}"))

		if (!dir.exists()) {
			dir.mkdirs()

			writeSpearmintConfig(dir)
			writeSpearmintPythonScript(dir)
		}
		dir
	}

	def writeSpearmintPythonScript(dir: File): Unit = {
		Some(new FileWriter(new File(s"$dir${File.separator}branin.py"))).foreach(f => {
			val pplibPath: String = overridePPLibPath.getOrElse(new File(".")).getAbsolutePath
			val featureList: List[String] = targetFeatures.map(f => f.path)
			f.write(createPythonScript(pplibPath, dir.getAbsolutePath, experimentName, featureList, sbtCommand, classOf[BOSpearmintEntrance].getCanonicalName))
			f.close()
		})
	}

	protected def writeSpearmintConfig(dir: File): Unit = {
		new SpearmintConfigExporter(expander).storeAsJson(new File(s"$dir${File.separator}config.json"), targetFeatures)
	}

	override def runOneIteration(input: INPUT): ExperimentResult = {
		import sys.process._
		s"$pythonCommand $pathToSpearmint${File.separator}spearmint${File.separator}main.py ${experimentPath.getAbsolutePath}" ! ProcessLogger(s => logger.info(s), s => logger.error(s))

		???
	}

	def createPythonScript(pplibPath: String, experimentPath: String, experimentName: String, variables: List[String], sbtCommand: String, pplibTargetClass: String): String =
		s"""
		   |import subprocess
		   |import string
		   |def main(job_id, params):
		   |    print 'Anything printed here will end up in the output directory for job #%d' % job_id
		   |    print params
		   |
		  |    target = open("jobdesc_%d.txt" % job_id, 'w')
		   |    ${variables.map(k => s"""target.write("$k" VALUE "+str(params["$k"][0])+"\n")""").mkString("\n    ")}
		   |    target.close()
		   |
		  |    cmd = 'cd $pplibPath && $sbtCommand "run-main $pplibTargetClass $experimentPath/jobdesc_'+str(job_id)+'.txt $experimentName"'
		   |    print "will execute "+cmd
		   |    p = subprocess.Popen(['/bin/bash', '-c', cmd], stdout=subprocess.PIPE)
		   |    p.wait()
		   |    output = p.stdout.read()
		   |    p.stdout.close()
		   |    print "process output was:"
		   |    print output
		   |    print "interpreting.."
		   |
		  |
		  |    lines = string.split(output, "\n")
		   |    relevantLine = lines[len(lines) - 3]
		   |    consoleString = string.split(relevantLine, " ")[1]
		   |    consolePrefix = len("\x1b[0m")
		   |    floatString = consoleString[consolePrefix:len(consoleString) - consolePrefix]
		   |
		  |    costLine = lines[len(lines) - 4]
		   |    linePrefix = "[0m[[0minfo[0m] [0mcost was"
		   |    print "process cost was " + costLine[len(linePrefix):len(costLine) - len(" [0m")]
		   |
		  |    print "got float string " + floatString
		   |    #return {'branin': float(floatString)}
		   |    return float(floatString)
		""".stripMargin
}

object BOSpearmintEntrance extends App {

}

class BOSpearmintEntrance {}