package ch.uzh.ifi.pdeboer.pplib.process

import java.io._

import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import org.joda.time.DateTime

@SerialVersionUID(1l) trait ProcessMemoizer extends Serializable {
	def processStub: ProcessStub[_, _]

	def name: String

	def overwriteExistingData: Boolean

	protected var snapshotList = List.empty[ProcessSnapshot]

	def addIncrementalSnapshot(name: String): ProcessSnapshot = {
		val s = addSnapshot(name)
		val olderElements: Map[String, Serializable] = if (latest.isDefined) latest.get.elements else Map()
		olderElements.foreach(e => s.addElement(e._1, e._2))
		s
	}

	def memWithReinitialization[T <: Serializable](name: String)(fn: => T)(reInitialization: T => T): T = {
		if (latest.isDefined && latest.get.elements.contains(name)) {
			val cacheHit = latest.get.elements(name).asInstanceOf[T]
			reInitialization(cacheHit)
		} else {
			val ret = fn
			addIncrementalSnapshot(name)
				.addElement(name, ret)
				.release()
			ret
		}
	}

	def mem[T <: Serializable](name: String)(fn: => T): T =
		memWithReinitialization(name)(fn)(t => t)

	def addSnapshot(name: String): ProcessSnapshot = {
		val r = new ProcessSnapshotImpl(name)
		synchronized {
			snapshotList = r :: snapshotList
		}
		r
	}

	def snapshots = snapshotList

	def latest = snapshotList.find(_.isFinal)

	def flush(): Boolean

	def load(): Boolean

	@SerialVersionUID(1l) trait ProcessSnapshot extends Serializable {
		val dateCreated: DateTime = DateTime.now()
		protected var content = collection.mutable.HashMap.empty[String, Serializable]

		protected var _isFinal: Boolean = false

		def isFinal = _isFinal

		def release() = {
			_isFinal = true
			flush()
			this
		}

		def addElement(key: String, value: Serializable) = {
			if (!_isFinal) {
				content += (key -> value)
			}
			this
		}

		def name: String

		def elements: Map[String, Serializable] = content.toMap

		def get[T](key: String) = elements(key).asInstanceOf[T]
	}

	protected class ProcessSnapshotImpl(val name: String) extends ProcessSnapshot {
		override def addElement(key: String, value: Serializable) = {
			super.addElement(key, value)
			flush()
			this
		}

		def canEqual(other: Any): Boolean = other.isInstanceOf[ProcessSnapshotImpl]

		override def equals(other: Any): Boolean = other match {
			case that: ProcessSnapshotImpl =>
				(that canEqual this) &&
					dateCreated == that.dateCreated &&
					content == that.content &&
					_isFinal == that._isFinal &&
					name == that.name

			case _ => false
		}

		override def hashCode(): Int = {
			val state = Seq(dateCreated, content, _isFinal, name)
			state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
		}
	}

}

class NoProcessMemoizer(val processStub: ProcessStub[_, _] = null, val name: String = "", val overwriteExistingData: Boolean = false) extends ProcessMemoizer {
	override def flush(): Boolean = true

	override def load(): Boolean = true

	override def memWithReinitialization[T <: Serializable](name: String)(fn: => T)(reInitialization: T => T): T = {
		fn
	}
}

/**
 * Created by pdeboer on 05/12/14.
 */
class FileProcessMemoizer(val processStub: ProcessStub[_, _], val name: String, val overwriteExistingData: Boolean = false) extends ProcessMemoizer with LazyLogger {
	val defaultPath = "state/"
	val defaultSuffix = ".state"

	val file = new File(defaultPath + name + defaultSuffix)
	if (overwriteExistingData) file.delete()
	if (file.exists()) load() else new File(defaultPath).mkdirs()

	private class SnapshotContainer(val snapshots: List[ProcessSnapshot]) extends Serializable

	override def flush(): Boolean = {
		synchronized {
			try {
				val foos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))
				foos.writeObject(new SnapshotContainer(snapshotList))
				foos.close()
				true
			}
			catch {
				case e: Exception => {
					logger.error(s"could not save process memoizer $name for ${processStub.getClass.getSimpleName}", e)
					false
				}
			}
		}
	}

	/**
	 * do not execute during runtime
	 * @return
	 */
	override def load(): Boolean = {
		synchronized {
			try {
				val fis = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))
				val container = fis.readObject().asInstanceOf[SnapshotContainer]
				snapshotList = container.snapshots
				fis.close()
				true
			}
			catch {
				case e: Exception => {
					logger.error(s"could not load process memoizer $name for ${processStub.getClass.getSimpleName}", e)
					false
				}
			}
		}
	}
}