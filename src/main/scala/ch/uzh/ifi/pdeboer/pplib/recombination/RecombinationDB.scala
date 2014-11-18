package ch.uzh.ifi.pdeboer.pplib.recombination

import ch.uzh.ifi.pdeboer.pplib.util.U
import com.typesafe.scalalogging.LazyLogging
import org.reflections.Reflections
import org.reflections.scanners.{ResourcesScanner, SubTypesScanner, TypeAnnotationsScanner}
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder, FilterBuilder}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.reflect.ClassTag


/**
 * Created by pdeboer on 20/10/14.
 */
object RecombinationDB extends LazyLogging {
	private var processes = mutable.HashMap.empty[RecombinationCategory, RecombinationCategoryContent]

	def reset(): Unit = {
		processes = mutable.HashMap.empty[RecombinationCategory, RecombinationCategoryContent]
	}

	def getCategory(category: RecombinationCategory, includeDescendants: Boolean = false, distinctProcesses: Boolean = false): Set[ProcessStub[_, _]] = {
		if (includeDescendants) {
			val keys = processes.keySet.filter(k => {
				//this probably wont work
				category.inputType.isAssignableFrom(k.inputType) &&
					category.outputType.isAssignableFrom(k.outputType) &&
					k.path.startsWith(category.path)
			})

			val instanciatedCandidates = keys.map(k => processes(k).stubs).flatten.toSet
			if (distinctProcesses) {
				//instanciatedCandidates.map(c => c.getClass.getCanonicalName -> c).toMap.values.toSet. Error?
				var ret = mutable.HashMap.empty[String, ProcessStub[_, _]]
				instanciatedCandidates.foreach(c => ret += c.getClass.getCanonicalName -> c)
				ret.values.toSet
			}
			else instanciatedCandidates
		} else {
			val pointSearchOption = processes.get(category)
			if (pointSearchOption.isDefined) pointSearchOption.get.stubs else Set.empty[ProcessStub[_, _]]
		}
	}

	def get[IN: ClassTag, OUT: ClassTag](name: String, includeDescendants: Boolean = false, distinctProcesses: Boolean = false): Set[ProcessStub[_, _]] = {
		val category: RecombinationCategory = RecombinationCategory.get[IN, OUT](name)
		getCategory(category, includeDescendants, distinctProcesses)
	}

	def put(stub: ProcessStub[_, _]): Unit = {
		stub.recombinationCategories.foreach(c => put(c, stub))
	}

	def put(category: RecombinationCategory, stub: ProcessStub[_, _]): Unit = {
		this.synchronized {
			val content = processes.getOrElse(category, RecombinationCategoryContent(category))
			content.addStub(stub)
			processes += (category -> content)
		}
	}

	def findClassesInPackageWithProcessAnnotation(packagePrefix: String): Set[Class[_]] = {
		val classLoadersList = List(ClasspathHelper.contextClassLoader(),
			ClasspathHelper.staticClassLoader())

		val reflections = new Reflections(new ConfigurationBuilder()
			.setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(false), new ResourcesScanner())
			.setUrls(ClasspathHelper.forClassLoader(classLoadersList(0)))
			.filterInputsBy(new FilterBuilder().include(
			FilterBuilder.prefix(packagePrefix))))

		reflections.getTypesAnnotatedWith(classOf[RecombinationProcess]).toSet
	}

	protected def findClassesInPackageWithAnnotationAndAddThem(packagePrefix: String = "ch.uzh.ifi.pdeboer.pplib.recombination.stdlib") {
		val annotatedClasses = findClassesInPackageWithProcessAnnotation(packagePrefix)
		initializeClassesAndAddToDB(annotatedClasses.asInstanceOf[Set[Class[ProcessStub[_, _]]]])
	}

	def initializeClassesAndAddToDB(classes: Set[Class[ProcessStub[_, _]]]) {
		classes.foreach(t => {
			try {
				val constructor = t.getConstructor(classOf[Map[String, Any]])
				constructor.newInstance(Map.empty[String, Any])
			}
			catch {
				case e: Error => logger.error("could not add class", System.err)
			}
		})
	}

	protected def autoloadPackagesFromConfigFile(): Unit = {
		U.getConfigString("processes.auto_init_package") match {
			case Some(targetPackage) => findClassesInPackageWithAnnotationAndAddThem(targetPackage)
			case _ => {}
		}
	}

	findClassesInPackageWithAnnotationAndAddThem()
	autoloadPackagesFromConfigFile()
}

case class RecombinationCategory(inputType: Class[_], outputType: Class[_], path: String) {}

object RecombinationCategory {
	def get[INPUT: ClassTag, OUTPUT: ClassTag](name: String) =
		new RecombinationCategory(
			implicitly[ClassTag[INPUT]].runtimeClass,
			implicitly[ClassTag[OUTPUT]].runtimeClass,
			name
		)
}

case class RecombinationCategoryContent(category: RecombinationCategory) {
	private var _stubs = Set.empty[ProcessStub[_, _]]

	def addStub(s: ProcessStub[_, _]): Unit = {
		_stubs += s
	}

	def stubs: Set[ProcessStub[_, _]] = _stubs
}