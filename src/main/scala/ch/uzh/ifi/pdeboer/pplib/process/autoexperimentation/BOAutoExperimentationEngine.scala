package ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation

import java.io._

import ch.uzh.ifi.pdeboer.pplib.process.entities.{SurfaceStructureFeatureExpander, XMLFeatureExpander}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{ResultWithUtility, SurfaceStructure}
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.joda.time.DateTime

import scala.io.Source

/**
  * Created by pdeboer on 16/03/16.
  */
class BOAutoExperimentationEngine[INPUT, OUTPUT <: ResultWithUtility](surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]],
																	  pathToSpearmint: File, experimentName: String, pathToSpearmintExperimentFolder: Option[File] = None, overridePPLibPath: Option[File] = None, sbtCommand: String = "sbt", pythonCommand: String = "python2.7", prefixPythonPathExport: Boolean = true, port: Int = 9988) extends AutoExperimentationEngine[INPUT, OUTPUT](surfaceStructures) {
	assert(experimentName != null && experimentName.length > 0)
	assert(pathToSpearmint.exists(), "Spearmint not found")

	protected val expander = new SurfaceStructureFeatureExpander(surfaceStructures)
	val targetFeatures = expander.featuresInclClass.filter(f => List("TypeTag[Int]", "TypeTag[Double]", XMLFeatureExpander.baseClassFeature.typeName).contains(f.typeName)).toList

	val JOB_QUEUE: String = "jobqueue"
	val JOB_QUEUE_DONE: String = "done"

	def createExperimentCommDirs(dir: File) = {
		new File(dir.getAbsolutePath + File.separator + JOB_QUEUE).mkdir()
		new File(dir.getAbsolutePath + File.separator + JOB_QUEUE_DONE).mkdir()
	}

	def jobQueueDir = experimentPath.getAbsolutePath + File.separator + JOB_QUEUE + File.separator

	def jobQueueDoneDir = experimentPath.getAbsolutePath + File.separator + JOB_QUEUE_DONE + File.separator

	def experimentPath = {
		val dir = pathToSpearmintExperimentFolder.getOrElse(
			new File(s"${pathToSpearmint.getAbsolutePath}${File.separator}examples${File.separator}$experimentName${File.separator}"))

		if (!dir.exists()) {
			dir.mkdirs()

			writeSpearmintConfig(dir)
			writeSpearmintPythonScript(dir)
			createExperimentCommDirs(dir)
		}
		dir
	}

	def writeSpearmintPythonScript(dir: File): Unit = {
		Some(new FileWriter(new File(s"$dir${File.separator}branin.py"))).foreach(f => {
			f.write(spearmintPythonScriptContent())
			f.close()
		})
	}

	protected def writeSpearmintConfig(dir: File): Unit = {
		new SpearmintConfigExporter(expander).storeAsJson(new File(s"$dir${File.separator}config.json"), targetFeatures)
	}

	def processSpearmintResult(entrance: BOSpearmintEntrance[INPUT, OUTPUT]): ExperimentResult = {
		ExperimentResult(entrance.results.map(r => ExperimentIteration(List(r))))
	}

	override def runOneIteration(input: INPUT): ExperimentResult = {
		import sys.process._

		val watchfolder = new File(experimentPath.getAbsolutePath + File.separator + JOB_QUEUE)
		val doneFolder = new File(experimentPath.getAbsolutePath + File.separator + JOB_QUEUE_DONE)

		val entrance = new BOSpearmintEntrance(input, watchfolder, doneFolder, expander, port)
		entrance.listen()

		//val exportCmd = if (prefixPythonPathExport) s"export PYTHONPATH='$pathToSpearmint' &&" else ""
		val cmd: String = s"$pythonCommand $pathToSpearmint${File.separator}spearmint${File.separator}main.py ${experimentPath.getAbsolutePath}"
		logger.info(s"will execute $cmd")
		cmd ! ProcessLogger(s => logger.info(s), s => logger.error(s))

		entrance.terminate()

		processSpearmintResult(entrance)
	}

	def spearmintPythonScriptContent() =
		s"""import socket
			|import os
			|import re
			|import time
			|
			 |
			 |def main(job_id, params):
			|    jobDesc = ""
			|    for key in params.keys():
			|        jobDesc += "%s VALUE %s\\n" % (key, params[key][0])
			|
 			|    f = open("${jobQueueDir}jobdesc_%s" % job_id, "w+")
			|    f.write(jobDesc)
			|    f.close()
			|
			|    outDir = "$jobQueueDoneDir"
			|
 			|    while (True):
			|        time.sleep(1)
			|        for aFile in os.listdir(outDir):
			|            if (aFile == ("answer_jobdesc_%s" % job_id)):
			|                ans = open(outDir+aFile, "r")
			|                utilityStringUnparsed = ans.readline()
			|                print("got line %s" % utilityStringUnparsed)
			|                ans.close()
			|                utilityString = re.sub(r"[^0-9\\.]", "", utilityStringUnparsed)
			|                print("parsed it to %s" % utilityString)
			|
 			|                return float(utilityString)
			| """.stripMargin
}

class BOSpearmintEntrance[INPUT, OUTPUT <: ResultWithUtility](input: INPUT, watchFolder: File, doneFolder: File, surfaceStructure: SurfaceStructureFeatureExpander[INPUT, OUTPUT], port: Int = 9988) extends LazyLogger {
	private var kill: Option[DateTime] = None

	protected var _results: List[SurfaceStructureResult[INPUT, OUTPUT]] = List.empty

	def results = _results

	def listen(): Unit = {
		new Thread() {
			override def run(): Unit = {
				while (surfaceStructure.synchronized(kill).isEmpty) {
					watchFolder.listFiles().filter(f => f.getName.startsWith("jobdesc")).foreach(f => {
						val inProgress: File = new File(doneFolder.getAbsolutePath + File.separator + f.getName)
						f.renameTo(inProgress)
						new SpearmintJobProcessor(inProgress).start()
					})
					Thread.sleep(500)
				}
			}
		}.start()
	}

	class SpearmintJobProcessor(val jobDescription: File) extends Thread with LazyLogger {

		override def run(): Unit = {
			val featureDefinition = Source.fromFile(jobDescription).getLines().map(l => {
				val content = l.split(" VALUE ")
				val valueAsOption = if (content(1) == "None") None else Some(content(1))
				surfaceStructure.featureByPath(content(0)).get -> valueAsOption
			}).toMap

			val targetSurfaceStructures = surfaceStructure.findSurfaceStructures(featureDefinition, exactMatch = false)
			val result = if (targetSurfaceStructures.isEmpty) None
			else {
				val res = targetSurfaceStructures.head.test(input)
				surfaceStructure.synchronized {
					_results = SurfaceStructureResult(targetSurfaceStructures.head, res) :: _results
				}
				res.map(_.utility)
			}

			val outputFileName: String = jobDescription.getParentFile.getAbsolutePath + File.separator + "answer_" + jobDescription.getName
			Some(new FileWriter(outputFileName)).foreach(f => {
				f.write(result.map(_.toString).getOrElse("1000"))
				f.close()
			})

		}
	}

	def terminate(): Unit = {
		surfaceStructure.synchronized {
			kill = Some(DateTime.now())
		}
	}
}