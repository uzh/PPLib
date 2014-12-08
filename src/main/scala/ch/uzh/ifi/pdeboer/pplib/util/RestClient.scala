package ch.uzh.ifi.pdeboer.pplib.util

import java.io.InputStream

import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpPut, HttpRequestBase}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

/**
 * Created by pdeboer on 17/11/14.
 */
abstract class RESTClient(val client: HttpClient) {
	var headers = scala.collection.mutable.HashMap.empty[String, String]


	lazy val responseBody: Future[HttpResult] = Future {
		val body: InputStream = response.getEntity.getContent
		HttpResult(
			Source.fromInputStream(body).getLines().mkString("\n"),
			response.getStatusLine.getStatusCode)
	}

	lazy val response = {
		val m = method
		headers.foreach {
			case (key, value) => m.addHeader(key, value)
		}
		client.execute(m)
	}

	def method: HttpRequestBase
}

case class HttpResult(body: String, statusCode: Int) {
	//406 is far fetched.. but CrowdFlower sometimes replies non-sense we don't care about
	def isOk = statusCode / 100 == 2 || List(302, 406).contains(statusCode)
}

abstract class RESTMethodWithBody(client: HttpClient) extends RESTClient(client) {
	var parameters = scala.collection.mutable.HashMap.empty[String, String]
	var bodyString: String = ""

	def body = if (bodyString == "") new UrlEncodedFormEntity(parameters.map {
		case (key, value) => new BasicNameValuePair(key, value)
	}.toList)
	else new StringEntity(bodyString)
}

class GET(url: String, client: HttpClient = HttpClientBuilder.create().build()) extends RESTClient(client) {
	override def method: HttpRequestBase = new HttpGet(url)
}

class PUT(_url: String, client: HttpClient = HttpClientBuilder.create().build()) extends RESTMethodWithBody(client) {
	def method = {
		val m = new HttpPut(_url)
		m.setEntity(body)
		m
	}
}

class POST(url: String, client: HttpClient = HttpClientBuilder.create().build()) extends RESTMethodWithBody(client) {
	def method = {
		val m = new HttpPost(url)
		m.setEntity(body)
		m
	}
}

class URLBuilder(protocol: String, host: String, port: Int, path: String) {
	protected var builder = new URIBuilder().setScheme(protocol).setHost(host).setPort(port).setPath(path)

	def addQueryParameter(key: String, value: String): URLBuilder = {
		builder = builder.addParameter(key, value)
		this
	}

	def /(part: String) = {
		builder = builder.setPath(builder.getPath + "/" + part)
		this
	}

	override def toString: String = builder.build().toString
}
