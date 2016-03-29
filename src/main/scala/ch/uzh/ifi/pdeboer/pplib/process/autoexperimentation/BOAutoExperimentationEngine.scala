package ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation

import java.io._

import ch.uzh.ifi.pdeboer.pplib.process.entities.{SurfaceStructureFeatureExpander, XMLFeatureExpander}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{ResultWithCostfunction, SurfaceStructure}
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}
import org.joda.time.DateTime

import scala.io.Source

/**
  * Created by pdeboer on 16/03/16.
  */
class BOAutoExperimentationEngine[INPUT, OUTPUT <: ResultWithCostfunction](surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]],
																		   pathToSpearmint: File, experimentName: String, pathToSpearmintExperimentFolder: Option[File] = None, pythonCommand: String = "python2.7",
																		   loadDataFromInterruptedRuns: Boolean = true, mongoDBPort: Int = 27017) extends AutoExperimentationEngine[INPUT, OUTPUT](surfaceStructures) {
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
		new SpearmintConfigExporter(expander).storeAsJson(new File(s"$dir${File.separator}config.json"), targetFeatures, experimentName, mongoDBPort)
	}

	def processSpearmintResult(entrance: BOSpearmintEntrance[INPUT, OUTPUT]): ExperimentResult = {
		ExperimentResult(entrance.results.map(r => ExperimentIteration(List(r))))
	}

	override def runOneIteration(input: INPUT): ExperimentResult = {
		import sys.process._

		val watchfolder = new File(experimentPath.getAbsolutePath + File.separator + JOB_QUEUE)
		val doneFolder = new File(experimentPath.getAbsolutePath + File.separator + JOB_QUEUE_DONE)

		val entrance = new BOSpearmintEntrance(input, watchfolder, doneFolder, expander)
		if (loadDataFromInterruptedRuns) entrance.loadDoneFolder()
		entrance.listen()

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

object BOAutoExperimentationEngine {
	val CONFIG_PREFIX: String = "autoexperimentation.bo."

	def createThroughConfig[INPUT, OUTPUT <: ResultWithCostfunction](surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]]) = {
		val spearmintPath = U.getConfigString(CONFIG_PREFIX + "spearmintPath").get
		val experimentName = U.getConfigString(CONFIG_PREFIX + "experimentName").getOrElse("PPLibExperiment")

		new BOAutoExperimentationEngine(surfaceStructures, new File(spearmintPath), experimentName)
	}
}

class BOSpearmintEntrance[INPUT, OUTPUT <: ResultWithCostfunction](input: INPUT, watchFolder: File, doneFolder: File, surfaceStructure: SurfaceStructureFeatureExpander[INPUT, OUTPUT]) extends LazyLogger {
	private var kill: Option[DateTime] = None

	protected var _results: List[SurfaceStructureResult[INPUT, OUTPUT]] = List.empty

	def results = _results

	def loadDoneFolder(): Unit = {
		doneFolder.listFiles().filter(_.getName.startsWith("jobdesc")).foreach(f => {
			try {
				val processor: SpearmintJobProcessor = new SpearmintJobProcessor(f)
				val output = Some(new ObjectInputStream(new FileInputStream(processor.resultObjectFileName))).map(f => {
					val out = f.readObject().asInstanceOf[Option[OUTPUT]]
					f.close()
					out
				}).get

				surfaceStructure.synchronized {
					_results = new SurfaceStructureResult(processor.correspondingSurfaceStructure.get, output) :: _results
				}
			} catch {
				case e: Throwable => logger.info(s"did not load $f", e.getMessage)
			}
		})
		logger.info(s"loaded ${_results.size} previous results")
	}

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
			val targetSurfaceStructures = correspondingSurfaceStructure
			val result = if (targetSurfaceStructures.isEmpty) None
			else {
				val res = targetSurfaceStructures.head.test(input)
				val structureResult = new SurfaceStructureResult(targetSurfaceStructures.head, res)
				surfaceStructure.synchronized {
					_results = structureResult :: _results
				}
				Some(structureResult)
			}

			Some(new FileWriter(outputFileName)).foreach(f => {
				val cost: Option[String] = result.flatMap(_.result.map(_.costFunctionResult.toString))
				f.write(cost.getOrElse("9999"))
				f.close()
			})

			Some(new ObjectOutputStream(new FileOutputStream(resultObjectFileName))).foreach(oos => {
				oos.writeObject(result.flatMap(_.result))
				oos.close()
			})
		}

		def outputFileName: File = {
			new File(jobDescription.getParentFile.getAbsolutePath + File.separator + "answer_" + jobDescription.getName)
		}

		def resultObjectFileName: File = {
			new File(jobDescription.getParentFile.getAbsolutePath + File.separator + "resultObject_" + jobDescription.getName)
		}

		def correspondingSurfaceStructure: Option[SurfaceStructure[INPUT, OUTPUT]] = {
			val featureDefinition = Source.fromFile(jobDescription).getLines().map(l => {
				val content = l.split(" VALUE ")
				val valueAsOption = if (content(1) == "None") None else Some(content(1))
				surfaceStructure.featureByPath(content(0)).get -> valueAsOption
			}).toMap

			val targetSurfaceStructures = surfaceStructure.findSurfaceStructures(featureDefinition, exactMatch = false)
			targetSurfaceStructures.headOption
		}
	}

	def terminate(): Unit = {
		surfaceStructure.synchronized {
			kill = Some(DateTime.now())
		}
	}
}