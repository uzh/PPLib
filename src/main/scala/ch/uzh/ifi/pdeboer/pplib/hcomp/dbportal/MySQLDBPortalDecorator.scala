package ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal

import java.util.Date

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompPortalAdapter, HCompQuery, HCompQueryProperties}
import ch.uzh.ifi.pdeboer.pplib.util.U
import scalikejdbc.{AutoSession, _}

/**
  * Created by pdeboer on 10/03/16.
  */
class MySQLDBPortalDecorator(val decorated: HCompPortalAdapter, processId: Option[Long] = None) extends HCompPortalAdapter {
	implicit val session = AutoSession

	createTable()

	def insertQueryAndAnswer(query: HCompQuery, answer: Option[HCompAnswer], hCompQueryProperties: HCompQueryProperties, retries: Int = 1): Unit = DB localTx { implicit session =>
		if (retries > 0) {
			try {
				sql"""
				INSERT INTO queries (process_id, question, fullQuery, answer, fullAnswer, paymentCents, fullProperties, questionCreationDate, questionAnswerDate, createDate, answerUser)
				VALUES (${processId.orNull}, ${query.question}, ${U.getJSON(query)}, ${answer.toString}, ${U.getJSON(answer)}, ${decorated.price(query, hCompQueryProperties)}, ${U.getJSON(hCompQueryProperties)}, ${answer.map(_.postTime).orNull}, ${answer.map(_.receivedTime).orNull}, ${new Date()}, ${answer.map(_.responsibleWorkers.map(_.id).toSet.mkString(","))})
		   """.update.apply()
			} catch {
				case e: Throwable => createTable(); insertQueryAndAnswer(query, answer, hCompQueryProperties, retries - 1)
			}
		}
	}

	def createTable(): Unit = DB localTx { implicit session =>
		try {
			sql"""CREATE TABLE IF NOT EXISTS `queries` (
			    `id` INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
				process_id INT(11) UNSIGNED NULL,
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
			  ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;""".update().apply()
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

	override def toString = s"MySQLDBPortalDecorator($decorated)"
}