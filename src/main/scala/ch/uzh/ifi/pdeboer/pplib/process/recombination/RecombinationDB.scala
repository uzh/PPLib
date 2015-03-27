package ch.uzh.ifi.pdeboer.pplib.process.recombination

import ch.uzh.ifi.pdeboer.pplib.process.entities.{PPLibProcess, ProcessStub}
import ch.uzh.ifi.pdeboer.pplib.util.{LazyLogger, U}

/**
 * Created by pdeboer on 17/02/15.
 */
class RecombinationDB {
	private var _classes = collection.mutable.HashSet.empty[Class[_ <: ProcessStub[_, _]]]

	def addClass(cls: Class[_ <: ProcessStub[_, _]]): Unit = {
		_classes += cls
	}

	def classes = _classes
}

class PPLibAnnotationLoader(target: RecombinationDB = RecombinationDB.DEFAULT) extends LazyLogger {
	protected def findClassesInPackageWithAnnotationAndAddThem(packagePrefix: String = "ch.uzh.ifi.pdeboer.pplib.process.stdlib", onlyAutoInit: Boolean = true) {
		val annotatedClasses = U.findClassesInPackageWithProcessAnnotation(packagePrefix, classOf[PPLibProcess])
		val filtered = if (onlyAutoInit) annotatedClasses.filter(_.getAnnotation(classOf[PPLibProcess]).autoInit()) else annotatedClasses
		initializeClassesAndAddToDB(filtered.asInstanceOf[Set[Class[ProcessStub[_, _]]]])
	}

	protected def initializeClassesAndAddToDB(classes: Set[Class[ProcessStub[_, _]]]) {
		classes.foreach(t => {
			target.addClass(t)
		})
	}

	protected def autoloadPackagesFromConfigFile(): Unit = {
		U.getConfigString("processes.auto_init_package") match {
			case Some(targetPackage) => findClassesInPackageWithAnnotationAndAddThem(targetPackage)
			case _ => {}
		}
	}

	def load(): Unit = {
		findClassesInPackageWithAnnotationAndAddThem()
		autoloadPackagesFromConfigFile()
	}

}

object RecombinationDB {
	val DEFAULT = new RecombinationDB
	try {
		new PPLibAnnotationLoader(DEFAULT).load()
	}
	catch {
		case e: Exception => {
			println("Couldn't auto-load processes in repository due to an error" + e.getMessage)
			e.printStackTrace()
		}
	}
}