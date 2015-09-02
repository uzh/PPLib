package ch.uzh.ifi.pdeboer.pplib.util

import java.lang.annotation
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

import ch.uzh.ifi.pdeboer.pplib.process.entities.ProcessStub
import com.typesafe.config.{Config, ConfigFactory}
import org.reflections.Reflections
import org.reflections.scanners.{ResourcesScanner, SubTypesScanner, TypeAnnotationsScanner}
import org.reflections.util.{ClasspathHelper, ConfigurationBuilder, FilterBuilder}

import scala.collection.JavaConversions._
import scala.collection.parallel.{ExecutionContextTaskSupport, ForkJoinTaskSupport, ParSeq}
import scala.concurrent.ExecutionContext
import scala.concurrent.forkjoin.ForkJoinPool
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Created by pdeboer on 15/10/14.
 */
object U extends LazyLogger {
	val hugeForkJoinPool = new ForkJoinPool(300)
	val hugeForkJoinTaskSupport: ForkJoinTaskSupport = new ForkJoinTaskSupport(hugeForkJoinPool)
	val execContextTaskSupport = new ExecutionContextTaskSupport(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(100)))

	val tinyForkJoinPool = new ForkJoinPool(1)

	def parallelify[T](seq: Seq[T]): ParSeq[T] = {
		val par = seq.par
		par.tasksupport = execContextTaskSupport
		par
	}

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
			case e if n > 1 => {
				logger.debug("retry got exception", e)
				retry(n - 1)(fn)
			}
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

	//taken from http://oldfashionedsoftware.com/2009/11/19/string-distance-and-refactoring-in-scala/
	def stringDistance(s1: String, s2: String): Int = {
		val memo = scala.collection.mutable.Map[(List[Char], List[Char]), Int]()
		def min(a: Int, b: Int, c: Int) = Math.min(Math.min(a, b), c)
		def sd(s1: List[Char], s2: List[Char]): Int = {
			if (memo.contains((s1, s2)) == false)
				memo((s1, s2)) = (s1, s2) match {
					case (_, Nil) => s1.length
					case (Nil, _) => s2.length
					case (c1 :: t1, c2 :: t2) => min(sd(t1, s2) + 1, sd(s1, t2) + 1,
						sd(t1, t2) + (if (c1 == c2) 0 else 1))
				}
			memo((s1, s2))
		}

		sd(s1.toList, s2.toList)
	}

	import scala.concurrent._

	def interruptableFuture[T](fun: () => T)(implicit ex: ExecutionContext): (Future[T], () => Boolean) = {
		val p = Promise[T]()
		val f = p.future
		val aref = new AtomicReference[Thread](null)
		p tryCompleteWith Future {
			val thread = Thread.currentThread
			aref.synchronized {
				aref.set(thread)
			}
			try fun() finally {
				val wasInterrupted = (aref.synchronized {
					aref getAndSet null
				}) ne thread
				//Deal with interrupted flag of this thread in desired
			}
		}

		(f, () => {
			aref.synchronized {
				Option(aref getAndSet null) foreach {
					_.interrupt()
				}
			}
			p.tryFailure(new CancellationException)
		})
	}

	def getAncestorsOfClass(clazz: Class[_], children: Set[Class[_]] = Set()): Set[Class[_]] = {
		if (clazz.getSuperclass == classOf[Any]) {
			children
		} else {
			val childrenToUse: Set[Class[_]] = if (children.size == 0) Set(clazz) else children
			getAncestorsOfClass(clazz.getSuperclass, childrenToUse.+(clazz.getSuperclass))
		}
	}

	def getTypeFromClass(c: Class[_]) =
		if (c == null) null else runtimeMirror(c.getClassLoader).classSymbol(c).toType

}

/**
 * this is pretty much the most horrible thing you'll see in this code :( but we haven't
 * found another way of easily manipulating runtimeClass of a ClassTag that's handed
 * over to a method/class.
 * @param clazz
 */
class SimpleClassTag[IN, OUT](clazz: Class[_]) extends ClassTag[ProcessStub[IN, OUT]] {
	override def runtimeClass: Class[_] = clazz
}
