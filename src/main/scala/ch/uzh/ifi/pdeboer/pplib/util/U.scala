package ch.uzh.ifi.pdeboer.pplib.util

import java.lang.annotation

import ch.uzh.ifi.pdeboer.pplib.recombination.PPLibProcess
import com.typesafe.config.{Config, ConfigFactory}
import org.reflections.Reflections
import org.reflections.scanners.{ResourcesScanner, SubTypesScanner, TypeAnnotationsScanner}
import org.reflections.util.{FilterBuilder, ConfigurationBuilder, ClasspathHelper}
import scala.collection.JavaConversions._
import scala.annotation.Annotation

/**
 * Created by pdeboer on 15/10/14.
 */
object U {
	/**
	 * Method used to retry some code that may fail n times.
	 * @param n  how often to retry
	 * @param fn  the fallible function
	 * @tparam T return value of the function
	 * @return the result of the function
	 */
	def retry[T](n: Int)(fn: => T): T = {
		try {
			fn
		} catch {
			case e if n > 1 =>
				retry(n - 1)(fn)
		}
	}

	def getConfigString(name: String): Option[String] = {
		val config: Config = ConfigFactory.load()
		try {
			Some(config.getString(name))
		}
		catch {
			case _ => None
		}
	}

	def findClassesInPackageWithProcessAnnotation(packagePrefix: String, anno: Class[_ <: annotation.Annotation]): Set[Class[_]] = {
		val classLoadersList = List(ClasspathHelper.contextClassLoader(),
			ClasspathHelper.staticClassLoader())

		val reflections = new Reflections(new ConfigurationBuilder()
			.setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(false), new ResourcesScanner())
			.setUrls(ClasspathHelper.forClassLoader(classLoadersList(0)))
			.filterInputsBy(new FilterBuilder().include(
			FilterBuilder.prefix(packagePrefix))))

		reflections.getTypesAnnotatedWith(anno).toSet
	}

	def removeWhitespaces(str: String) = str.replaceAll("\\s*", "")
}
