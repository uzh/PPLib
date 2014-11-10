package ch.uzh.ifi.pdeboer.pplib.recombination

import ch.uzh.ifi.pdeboer.pplib.recombination.stdlib.DualPathwayProcess
import org.reflections.Reflections
import org.reflections.scanners.{ResourcesScanner, SubTypesScanner, TypeAnnotationsScanner}
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder, FilterBuilder}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.reflect.ClassTag


/**
 * Created by pdeboer on 20/10/14.
 */
object RecombinationDB {
	//TODO implement recomb db
	def get[IN: ClassTag, OUT: ClassTag](simpleName: String) = ???

	def put(category: RecombinationCategory, stub: RecombinationStub[_, _]): Unit = ???

	def findClassesThatExtendRecombinationStubAndAddThem(): Unit = {
		try {
			val classLoadersList = List(ClasspathHelper.contextClassLoader(), ClasspathHelper.staticClassLoader())

			val reflections = new Reflections(new ConfigurationBuilder()
				.setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(false), new ResourcesScanner())
				.setUrls(ClasspathHelper.forClassLoader(classLoadersList(0)))
				.filterInputsBy(new FilterBuilder().include(
				FilterBuilder.prefix(DualPathwayProcess.getClass.getPackage.getName))))

			val target = reflections.getTypesAnnotatedWith(classOf[RecombinationProcess])
			target.foreach(_.newInstance()) // recombination stub will automatically add itself to DB
		}
		catch {
			case e: Exception => e.printStackTrace()
		}
	}

	findClassesThatExtendRecombinationStubAndAddThem()
}

class RecombinationCategory(val inputType: Class[_], val outputType: Class[_], val name: String) {}

object RecombinationCategory {
	def get[INPUT: ClassTag, OUTPUT: ClassTag](name: String) =
		new RecombinationCategory(
			implicitly[ClassTag[INPUT]].runtimeClass,
			implicitly[ClassTag[OUTPUT]].runtimeClass,
			name
		)
}

object TestStuff extends App {
	RecombinationDB.findClassesThatExtendRecombinationStubAndAddThem()
}

case class RecombinationSetting[IN, OUT]()

case class RecombinationCategoryContent(name: String) {
	private var _stubs: mutable.Set[RecombinationStub[_, _]] = mutable.HashSet.empty[RecombinationStub[_, _]]

	def addStub(s: RecombinationStub[_, _]): Unit = {
		_stubs += s
	}

	def stubs = _stubs.toSet
}

object RecombinationCategoryContent {
	def generateKey[INPUT: ClassTag, OUTPUT: ClassTag](name: String): String =
		s"in:${implicitly[ClassTag[INPUT]].runtimeClass.getSimpleName},out:${implicitly[ClassTag[OUTPUT]].runtimeClass.getSimpleName},name:$name"
}