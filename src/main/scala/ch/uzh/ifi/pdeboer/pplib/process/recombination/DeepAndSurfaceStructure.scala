package ch.uzh.ifi.pdeboer.pplib.process.recombination

import java.util.Date

import ch.uzh.ifi.pdeboer.pplib.process.entities.{ProcessFeature, ProcessStub, SurfaceStructureFeatureExpander}
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}
import scalikejdbc.{AutoSession, ConnectionPool}

/**
  * Created by pdeboer on 09/10/14.
  */
trait DeepStructure[INPUT, OUTPUT <: ResultWithCostfunction] {
	def run(data: INPUT, recombinedProcessBlueprint: RecombinedProcessBlueprint): OUTPUT

	def defineRecombinationSearchSpace: Map[String, RecombinationSearchSpaceDefinition[_]]
}

trait SimpleDeepStructure[INPUT, OUTPUT <: ResultWithCostfunction] extends DeepStructure[INPUT, OUTPUT] {

	import SimpleDeepStructure._

	def defineSimpleRecombinationSearchSpace: RecombinationSearchSpaceDefinition[_ <: ProcessStub[_, _]]

	override def defineRecombinationSearchSpace: Map[String, RecombinationSearchSpaceDefinition[_]] = {
		Map(DEFAULT_KEY -> defineSimpleRecombinationSearchSpace)
	}
}

object SimpleDeepStructure {
	val DEFAULT_KEY: String = ""
}

class SurfaceStructure[INPUT, OUTPUT <: ResultWithCostfunction](val deepStructure: DeepStructure[INPUT, OUTPUT], val recombinedProcessBlueprint: RecombinedProcessBlueprint) extends LazyLogger {
	def test(data: INPUT): Option[OUTPUT] = try {
		Some(deepStructure.run(data, recombinedProcessBlueprint))
	}
	catch {
		case e: Exception => {
			logger.error(s"An error occurred when testing the deep structure with recombined process $recombinedProcessBlueprint and data $data", e)
			None
		}
	}

	override def toString = s"SurfaceStructure($recombinedProcessBlueprint)"
}

trait SurfaceStructureResultListener[INPUT, OUTPUT <: ResultWithCostfunction] {
	def handleSurfaceStructureResult(surfaceStructure: SurfaceStructure[INPUT, OUTPUT], input: INPUT, result: Option[OUTPUT])

	def handleSurfaceStructureException(surfaceStructure: SurfaceStructure[INPUT, OUTPUT], input: INPUT, e: Exception)
}

class MySQLSurfaceStructureResultListener[INPUT, OUTPUT <: ResultWithCostfunction](surfaceStructurePersistor: MySQLSurfaceStructurePersistor[INPUT, OUTPUT], mysqlUser: String = "root", mysqlPassword: String = "", mysqlHost: String = "127.0.0.1", mysqlDB: String = "pplibQueries") extends SurfaceStructureResultListener[INPUT, OUTPUT] {
	Class.forName("com.mysql.jdbc.Driver")
	ConnectionPool.singleton(s"jdbc:mysql://$mysqlHost/$mysqlDB", mysqlUser, mysqlPassword)
	implicit val session = AutoSession

	import scalikejdbc._

	override def handleSurfaceStructureResult(surfaceStructure: SurfaceStructure[INPUT, OUTPUT], input: INPUT, result: Option[OUTPUT]): Unit = {
		try {
			insertResult(surfaceStructure, input, result)
		} catch {
			case e: Throwable => createTable(); insertResult(surfaceStructure, input, result)
		}
	}

	override def handleSurfaceStructureException(surfaceStructure: SurfaceStructure[INPUT, OUTPUT], input: INPUT, exception: Exception): Unit = {
		try {
			insertResult(surfaceStructure, input, exc = exception)
		} catch {
			case e: Throwable => createTable(); insertResult(surfaceStructure, input, exc = exception)
		}
	}

	def insertResult(surfaceStructure: SurfaceStructure[INPUT, OUTPUT], input: INPUT, result: Option[OUTPUT] = None, exc: Exception = null, retries: Int = 1): Unit = DB localTx { implicit session =>
		if (retries > 0) {
			val surfaceStructureId = surfaceStructurePersistor.getIdOfSurfaceStructureFromDB(surfaceStructure)
			val excJSON = if (exc != null) U.getJSON(exc) else ""
			val excString = if (exc != null) exc.toString else ""
			try {
				sql"""INSERT INTO surface_structure_results (surface_structure_id, input_json, input_string, result_json, result_string,
					 result_exception_json, result_exception_string, create_date)
					 VALUES ($surfaceStructureId, ${U.getJSON(input)}, ${input.toString}, ${U.getJSON(result)}, ${result.toString},
						$excJSON, $excString, ${new Date()})""".update.apply()
			} catch {
				case e: Throwable => createTable(); insertResult(surfaceStructure, input, result, exc, retries - 1)
			}
		}
	}

	def createTable(): Unit = DB localTx { implicit session =>
		try {
			sql"""CREATE TABLE IF NOT EXISTS `surface_structure_results` (
			    `result_id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
			    `surface_structure_id` INT(11) UNSIGNED NOT NULL ,
	   			`input_json` LONGTEXT,
	   			`input_string` LONGTEXT,
			    `result_json` LONGTEXT,
			    `result_string` LONGTEXT,
	   			`result_exception_json` LONGTEXT,
	   			`result_exception_string` LONGTEXT,
			    `create_date` DATETIME DEFAULT NULL,
			    PRIMARY KEY (`result_id`)
			  ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;""".update().apply()
		} catch {
			case e: Throwable => {}
		}
	}
}

class MySQLSurfaceStructurePersistor[INPUT, OUTPUT <: ResultWithCostfunction](expander: SurfaceStructureFeatureExpander[INPUT, OUTPUT], targetFeatures: Option[List[ProcessFeature]] = None, mysqlUser: String = "root", mysqlPassword: String = "", mysqlHost: String = "127.0.0.1", mysqlDB: String = "pplibQueries") {
	Class.forName("com.mysql.jdbc.Driver")
	ConnectionPool.singleton(s"jdbc:mysql://$mysqlHost/$mysqlDB", mysqlUser, mysqlPassword)
	implicit val session = AutoSession

	import scalikejdbc._

	lazy val surfaceStructureIDs: Map[SurfaceStructure[INPUT, OUTPUT], Long] = {
		createTable()
		expander.surfaceStructures.map(ss => {
			ss -> getIdOfSurfaceStructureFromDB(ss).getOrElse(insertSurfaceStructure(ss))
		}).toMap
	}

	def insertSurfaceStructure(surfaceStructure: SurfaceStructure[INPUT, OUTPUT]) = DB localTx { implicit session =>
		val newSurfaceStructureID = highestSurfaceStructureID + 1
		val surfaceStructureJSON = U.getJSON(surfaceStructure)
		targetFeatures.getOrElse(expander.features).foreach(processFeature => {
			val featureName = uniqueSQLNameOfFeature(processFeature)
			val featureValue = expander.featureValueAt(processFeature, surfaceStructure)
			sql"""INSERT INTO surface_structure (surface_structure_id, feature_name, feature_value, surface_structure_json, create_date)
				  VALUES ($newSurfaceStructureID, $featureName, $featureValue, $surfaceStructureJSON, ${new Date()})
			   """.update().apply()
		})
		newSurfaceStructureID
	}

	def highestSurfaceStructureID: Long = DB localTx { implicit session => sql"SELECT MAX(surface_structure_id) AS s FROM surface_structure".map(s => s.long(1)).single().apply().getOrElse(-1L) }

	def getIdOfSurfaceStructureFromDB(surfaceStructure: SurfaceStructure[INPUT, OUTPUT]) = DB localTx { implicit session =>
		val ids = targetFeatures.getOrElse(expander.features).map(processFeature => {
			val featureName = uniqueSQLNameOfFeature(processFeature)
			val featureValue = expander.featureValueAt(processFeature, surfaceStructure)
			sql"""SELECT surface_structure_id FROM surface_structure WHERE feature_name = ${featureName} AND feature_value=${featureValue}""".map(rs => rs.long(1)).list().apply()
		}).toList.sortBy(_.length)

		val remainingIds = ids.foldLeft(ids.headOption.getOrElse(Nil))((remainingIDs, currentList) => remainingIDs.filterNot(rid => currentList.contains(rid)))
		assert(remainingIds.size < 2, "found more than one surface structure for this set of features. that shouldnt be")

		remainingIds.headOption
	}

	lazy val uniqueSQLNameOfFeature = targetFeatures.getOrElse(expander.features).map(f => (f.path.replaceAll("[^a-zA-Z0-9]", "_"), f)).groupBy(_._1).flatMap(g => g._2.zipWithIndex.map(gf => {
		val feature: ProcessFeature = gf._1._2
		val cleanFeatureName: String = gf._1._1
		val suffix: String = if (g._2.size > 1) gf._2.toString else ""
		feature -> (cleanFeatureName + suffix)
	}
	)).toMap

	def createTable(): Unit = DB localTx { implicit session =>
		try {
			sql"""CREATE TABLE IF NOT EXISTS `surface_structure` (
			    `surface_structure_option_id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
			    `surface_structure_id` INT(11) UNSIGNED NOT NULL ,
	   			`feature_name` VARCHAR(255) NOT NULL,
	   			`feature_value` LONGTEXT,
				`surface_structure_json` LONGTEXT,
			    `create_date` DATETIME DEFAULT NULL,
			    PRIMARY KEY (`surface_structure_option_id`),
	   			KEY surface_structure_id (surface_structure_id)
			  ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;""".update().apply()
		} catch {
			case e: Throwable => {}
		}
	}
}