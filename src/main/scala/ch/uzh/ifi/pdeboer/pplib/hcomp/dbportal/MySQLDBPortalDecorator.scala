package ch.uzh.ifi.pdeboer.pplib.hcomp.dbportal

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

		sql"""
				INSERT INTO queries (question, fullQuery, answer, fullAnswer, paymentCents, fullProperties, questionCreationDate, questionAnswerDate, answerUser)
				VALUES ( ${query.question}, ${getJSON(query)}, ${answer.toString}, ${getJSON(answer)}, ${hCompQueryProperties.paymentCents}, ${getJSON(hCompQueryProperties)}, ${answer.map(_.postTime)}, ${answer.map(_.receivedTime)}, ${answer.map(_.responsibleWorkers.map(_.id).mkString(","))})
		   """.update.apply()
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