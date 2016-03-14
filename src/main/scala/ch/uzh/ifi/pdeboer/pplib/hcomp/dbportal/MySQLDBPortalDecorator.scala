package ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal

import java.util.Date

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompPortalAdapter, HCompQuery, HCompQueryProperties}
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import scalikejdbc.{AutoSession, ConnectionPool, _}

/**
  * Created by pdeboer on 10/03/16.
  */
class MySQLDBPortalDecorator(decorated: HCompPortalAdapter, mysqlUser: String = "root", mysqlPassword: String = "", mysqlHost: String = "127.0.0.1", mysqlDB: String = "pplibQueries") extends HCompPortalAdapter {
	Class.forName("com.mysql.jdbc.Driver")
	ConnectionPool.singleton(s"jdbc:mysql://$mysqlHost/$mysqlDB", mysqlUser, mysqlPassword)
	implicit val session = AutoSession

	def insertQueryAndAnswer(query: HCompQuery, answer: Option[HCompAnswer], hCompQueryProperties: HCompQueryProperties): Unit = DB localTx { implicit session =>
		def getJSON(obj: Any): String = {
			val mapper = new ObjectMapper()
			mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY)
			mapper.writeValueAsString(obj)
		}

		try {
			sql"""
				INSERT INTO queries (question, fullQuery, answer, fullAnswer, paymentCents, fullProperties, questionCreationDate, questionAnswerDate, createDate, answerUser)
				VALUES ( ${query.question}, ${getJSON(query)}, ${answer.toString}, ${getJSON(answer)}, ${hCompQueryProperties.paymentCents}, ${getJSON(hCompQueryProperties)}, ${answer.map(_.postTime).orNull}, ${answer.map(_.receivedTime).orNull}, ${new Date()}, ${answer.map(_.responsibleWorkers.map(_.id).toSet.mkString(","))})
		   """.update.apply()
		} catch {
			case e: Throwable => createLayout()
		}
	}

	def createLayout(): Unit = DB localTx { implicit session =>
		try {
			sql"""CREATE TABLE IF NOT EXISTS `queries` (
			    `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
			    `question` LONGTEXT,
			    `fullQuery` LONGTEXT,
			    `answer` LONGTEXT,
			    `fullAnswer` LONGTEXT,
			    `paymentCents` INT(11) DEFAULT NULL,
			    `fullProperties` LONGTEXT,
			    `questionCreationDate` DATETIME DEFAULT NULL,
				 `questionAnswerDate` DATETIME DEFAULT NULL,
				 `createDate` DATETIME DEFAULT NULL,
			    `answerUser` VARCHAR(255) DEFAULT NULL,
			    PRIMARY KEY (`id`)
			  ) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;""".update().apply()
		} catch {
			case e: Throwable => {}
		}
	}

	override def processQuery(query: HCompQuery, properties: HCompQueryProperties): Option[HCompAnswer] = {
		val answer = decorated.processQuery(query, properties)
		insertQueryAndAnswer(query, answer, properties)
		answer
	}

	override def cancelQuery(query: HCompQuery): Unit = {
		decorated.cancelQuery(query)
	}
}