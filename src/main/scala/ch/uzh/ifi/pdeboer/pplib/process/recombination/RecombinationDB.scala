package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PPLibProcess, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}

/**
 * Created by pdeboer on 17/02/15.
 */
class RecombinationDB {
	private var classes = collection.mutable.HashSet.empty[Class[_ <: ProcessStub[_, _]]]

	def addClass(cls: Class[_ <: ProcessStub[_, _]]): Unit = {
		classes += cls
	}
}

class PPLibAnnotationLoader(target: RecombinationDB) extends LazyLogger {
	protected def findClassesInPackageWithAnnotationAndAddThem(packagePrefix: String = "ch.uzh.ifi.pdeboer.pplib.process.stdlib", onlyAutoInit: Boolean = true) {
		val annotatedClasses = U.findClassesInPackageWithProcessAnnotation(packagePrefix, classOf[PPLibProcess])
		val filtered = if (onlyAutoInit) annotatedClasses.filter(_.getAnnotation(classOf[PPLibProcess]).autoInit()) else annotatedClasses
		initializeClassesAndAddToDB(filtered.asInstanceOf[Set[Class[ProcessStub[_, _]]]])
	}

	def initializeClassesAndAddToDB(classes: Set[Class[ProcessStub[_, _]]]) {
		classes.foreach(t => {
			try {
				val annotation = t.getAnnotation(classOf[PPLibProcess])
				//val builder = if(annotation.builder() == null) new DefaultProcessFactory(t) else annotation.builder()
				//TODO build and add
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

object RecombinationDB {
	val DEFAULT = new RecombinationDB

	//load all annotated classes and add them here
}