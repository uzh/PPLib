package ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation

import java.io._
import java.net.{ServerSocket, Socket}

import ch.uzh.ifi.pdeboer.pplib.examples.optimizationSimulation.SpearmintConfigExporter
import ch.uzh.ifi.pdeboer.pplib.process.entities.{SurfaceStructureFeatureExpander, XMLFeatureExpander}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{ResultWithUtility, SurfaceStructure}
import org.joda.time.DateTime

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

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
			f.write(createPythonScript(pplibPath, dir.getAbsolutePath, experimentName, featureList, sbtCommand, classOf[BOSpearmintEntrance[_, _]].getCanonicalName))
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

		val entrance = new BOSpearmintEntrance(input, expander)
		entrance.listen()

		s"$pythonCommand $pathToSpearmint${File.separator}spearmint${File.separator}main.py ${experimentPath.getAbsolutePath}" ! ProcessLogger(s => logger.info(s), s => logger.error(s))

		entrance.terminate()

		processSpearmintResult(entrance)
	}

	def sockPythonScriptContent(variables: List[String], port: Int = 9988) =
		s"""
		   |import socket
		   |
 			|def main(job_id, params):
		   |    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		   |    sock.connect( ("127.0.0.1", 9988) )
		   |    sock.send(u"all key VALUE value\n")
		   |    utility = sock.recv(1024)
		   |    return float(utility)
		 """.stripMargin


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
		   |    print "process serialization was " + costLine[len(linePrefix):len(costLine) - len(" [0m")]
		   |
		  |    print "got float string " + floatString
		   |    #return {'branin': float(floatString)}
		   |    return float(floatString)
		""".stripMargin
}

object BOSpearmintEntrance extends App {
	assert(this.getClass.getCanonicalName == classOf[BOSpearmintEntrance[_, _]].getCanonicalName, "always needs to have the same name as class.")


}

class BOSpearmintEntrance[INPUT, OUTPUT <: ResultWithUtility](input: INPUT, surfaceStructure: SurfaceStructureFeatureExpander[INPUT, OUTPUT], port: Int = 9988) {
	private var kill: Option[DateTime] = None

	protected var openThreads: mutable.Set[SpearmintSocketProcessor] = mutable.Set.empty

	protected var _results: List[SurfaceStructureResult[INPUT, OUTPUT]] = List.empty

	def results = _results.toList

	def listen(): Unit = {
		new Thread() {
			override def run(): Unit = {
				val server = new ServerSocket(port)
				while (surfaceStructure.synchronized(kill).isEmpty) {
					val socket = server.accept()
					new SpearmintSocketProcessor(socket).start()
				}
			}
		}
	}

	class SpearmintSocketProcessor(val socket: Socket) extends Thread {
		protected val rand = Random.nextLong()

		override def run(): Unit = {
			surfaceStructure.synchronized {
				openThreads += this
			}

			val featureDefinition = Source.fromInputStream(socket.getInputStream).getLines().map(l => {
				val content = l.split(" VALUE ")
				val valueAsOption = if (content(1) == "None") None else Some(content(1))
				surfaceStructure.featureByPath(content(0)).get -> valueAsOption
			}).toMap

			val targetSurfaceStructures = surfaceStructure.findSurfaceStructures(featureDefinition, exactMatch = false)
			if (targetSurfaceStructures.isEmpty) println(10000.0)
			else {
				val res = targetSurfaceStructures.head.test(input)
				surfaceStructure.synchronized {
					_results = SurfaceStructureResult(targetSurfaceStructures.head, res) :: _results
				}
				val os = new OutputStreamWriter(socket.getOutputStream)
				os.write("" + res.map(_.utility).getOrElse("1000"))
				os.close()
			}

			surfaceStructure.synchronized {
				openThreads -= this
			}
		}

		def canEqual(other: Any): Boolean = other.isInstanceOf[SpearmintSocketProcessor]

		override def equals(other: Any): Boolean = other match {
			case that: SpearmintSocketProcessor =>
				(that canEqual this) &&
					rand == that.rand
			case _ => false
		}

		override def hashCode(): Int = {
			val state = Seq(rand)
			state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
		}
	}

	def terminate(): Unit = {
		synchronized {
			kill = Some(DateTime.now())
		}
		new Thread {
			override def run(): Unit = {
				Thread.sleep(5000)
				val threadsCopy = surfaceStructure.synchronized(openThreads.toSet)
				threadsCopy.foreach(t => {
					try {
						t.stop() //bad bad me
					} catch {
						case t: Throwable => {}
					}
				})
			}
		}.start()
	}
}