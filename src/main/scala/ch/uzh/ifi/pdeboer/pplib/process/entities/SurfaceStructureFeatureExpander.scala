package ch.uzh.ifi.pdeboer.pplib.process.entities

import ch.uzh.ifi.pdeboer.pplib.process.recombination.SurfaceStructure
import com.github.tototoshi.csv.CSVWriter

import scala.xml.{Node, NodeSeq}

case class ProcessFeature(name: String, path: String, typeName: String)

class XMLFeatureExpander(xmls: List[NodeSeq]) {

	import XMLFeatureExpander._

	def valueAtPath(xml: NodeSeq, path: String): Option[String] = {
		if (path == baseClassFeature.path)
			Some((xml \ "Class").text.trim)
		else {
			val pathNoPrefix = if (path.charAt(0) == '/') path.substring(1) else path
			val indexOfNextPathDelimiter: Int = pathNoPrefix.indexOf("/")
			val isLastElementOfPath: Boolean = indexOfNextPathDelimiter == -1
			val targetName = if (isLastElementOfPath) pathNoPrefix else pathNoPrefix.substring(0, indexOfNextPathDelimiter)
			val value = (xml \ "Parameters" \ "Parameter").find(p => (p \ "Name").text.trim == targetName)

			def getParamValue(nodeSeq: NodeSeq): String = if ((nodeSeq \ "Process").isEmpty) nodeSeq.text.trim else getParamValue(nodeSeq \ "Process" \ "Class")

			if (value.isDefined) {
				val xmlValue: NodeSeq = value.get \ "Value"

				if (isLastElementOfPath) Some(getParamValue(xmlValue))
				else valueAtPath(xmlValue \ "Process", pathNoPrefix.substring(indexOfNextPathDelimiter))
			} else None
		}
	}

	lazy val features: Set[ProcessFeature] = {
		xmls.map(x => extractFeaturesFromProcessXML(x.theSeq.head, "")).toSet.flatten
	}

	def featuresInclClass = {
		features + baseClassFeature
	}

	def extractFeaturesFromProcessXML(xml: Node, prefix: String): Set[ProcessFeature] = {
		val params = (xml \ "Parameters" \ "Parameter").map(p => {
			val name: String = (p \ "Name").text.trim
			val newPrefix: String = prefix + "/" + name
			val currentFeature = ProcessFeature(newPrefix, newPrefix, (p \ "Type").text.trim)
			if ((p \\ "Process").isEmpty) {
				Set(currentFeature)
			}
			else
				extractFeaturesFromProcessXML((p \ "Value" \ "Process").theSeq.head, newPrefix) + currentFeature
		}).toSet
		params.flatten
	}
}

object XMLFeatureExpander {
	val baseClassFeature: ProcessFeature = ProcessFeature("baseclass", "baseclass", "++baseclass")

}

/**
  * Created by pdeboer on 10/12/15.
  */
class SurfaceStructureFeatureExpander[INPUT, OUTPUT <: Comparable[OUTPUT]](val surfaceStructures: List[SurfaceStructure[INPUT, OUTPUT]]) extends XMLFeatureExpander(surfaceStructures.map(s => s.recombinedProcessBlueprint.createProcess().xml)) {
	def featureValueAt(feature: ProcessFeature, surfaceStructure: SurfaceStructure[INPUT, OUTPUT]) = valueAtPath(surfaceStructure.recombinedProcessBlueprint.createProcess().xml, feature.path)

	def findSurfaceStructures(filter: Map[ProcessFeature, Option[String]], exactMatch: Boolean = true) = {
		import XMLFeatureExpander._
		if (exactMatch) surfaceStructures.filter(ss => filter.forall(fi => featureValueAt(fi._1, ss) == fi._2))
		else {
			List(surfaceStructures.filter(ss => featureValueAt(baseClassFeature, ss) == filter(baseClassFeature))
				.map(ss => (ss, filter.count(fi => featureValueAt(fi._1, ss) == fi._2)))
				.sortBy(-_._2)
				.head
				._1)
		}
	}

	def featureByPath(path: String) = featuresInclClass.find(_.path == path)

	def toCSV(file: String, targetFeatures: List[ProcessFeature] = features.toList, dataToInject: Map[SurfaceStructure[INPUT, OUTPUT], List[Any]] = Map.empty): Unit = {
		val wr = CSVWriter.open(file)
		wr.writeRow(targetFeatures.map(_.name))
		surfaceStructures.foreach(s => {
			wr.writeRow(targetFeatures.map(f => featureValueAt(f, s).getOrElse("")) ::: dataToInject.getOrElse(s, Nil))
		})
		wr.close()
	}
}
