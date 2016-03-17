package ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation

import java.io._
import java.net.{ServerSocket, Socket}
import ch.uzh.ifi.pdeboer.pplib.process.entities.{SurfaceStructureFeatureExpander, XMLFeatureExpander}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.{ResultWithUtility, SurfaceStructure}
import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.joda.time.DateTime

import scala.collection.mutable
import scala.io.Source
import scala.util.Random

/**
  * Created by pdeboer on 16/03/16.
  */
class BOAutoExperimentationEngine[INPUT, OUTPUT <: ResultWithUtility](surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]],
																	  pathToSpearmint: File, experimentName: String, pathToSpearmintExperimentFolder: Option[File] = None, overridePPLibPath: Option[File] = None, sbtCommand: String = "sbt", pythonCommand: String = "python2.7", prefixPythonPathExport: Boolean = true, port: Int = 9988) extends AutoExperimentationEngine[INPUT, OUTPUT](surfaceStructures) {
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

		val entrance = new BOSpearmintEntrance(input, expander, port)
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
			|
			 |
			 |def main(job_id, params):
			|    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			|    sock.connect(("127.0.0.1", $port))
			|    strToSend = u""
			|    for key in params.keys():
			|            strToSend += u"%s VALUE %s\n" % (key,params[key][0])
			|    print("sent %s" % strToSend)
			|    sock.sendall(strToSend)
			|    utility = sock.recv(4096)
			|    print("received %s" % utility)
			|    socket.close()
			|    return float(utility) """.stripMargin
}

class BOSpearmintEntrance[INPUT, OUTPUT <: ResultWithUtility](input: INPUT, surfaceStructure: SurfaceStructureFeatureExpander[INPUT, OUTPUT], port: Int = 9988) extends LazyLogger {
	private var kill: Option[DateTime] = None

	protected var openThreads: mutable.Set[SpearmintSocketProcessor] = mutable.Set.empty

	protected var _results: List[SurfaceStructureResult[INPUT, OUTPUT]] = List.empty

	def results = _results.toList

	def listen(): Unit = {
		new Thread() {
			override def run(): Unit = {
				val server = new ServerSocket(port)
				logger.info(s"starting pplib bo server at $port")
				while (surfaceStructure.synchronized(kill).isEmpty) {
					logger.info("waiting for connections..")
					val socket = server.accept()
					logger.info(s"got connection: $socket")
					new SpearmintSocketProcessor(socket).start()
				}
			}
		}.start()
	}

	class SpearmintSocketProcessor(val socket: Socket) extends Thread with LazyLogger {
		protected val rand = Random.nextLong()

		override def run(): Unit = {
			surfaceStructure.synchronized {
				openThreads += this
			}

			val lines = Source.fromInputStream(socket.getInputStream).getLines().toList
			logger.info(s"received message $lines")

			val featureDefinition = lines.map(l => {
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