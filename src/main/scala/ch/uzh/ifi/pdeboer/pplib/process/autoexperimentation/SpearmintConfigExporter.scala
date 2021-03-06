package ch.uzh.ifi.pdeboer.pplib.process.autoexperimentation

/**
  * Created by pdeboer on 13/12/15.
  */

import java.io.{File, FileWriter}

import ch.uzh.ifi.pdeboer.pplib.process.entities.{ProcessFeature, SurfaceStructureFeatureExpander}
import ch.uzh.ifi.pdeboer.pplib.process.recombination.ResultWithCostfunction

class SpearmintConfigExporter[INPUT, OUTPUT <: ResultWithCostfunction](featureExpander: SurfaceStructureFeatureExpander[INPUT, OUTPUT]) {
	/*
		 "X":{"type"    : "ENUM",
		  "size"    : 2,
		  "options" : ["one", "two", "three"]}
	*/

	def enumRangeFor(feature: ProcessFeature): List[String] = {
		val range = featureExpander.surfaceStructures.groupBy(s => featureExpander.featureValueAt(feature, s))

		range.filter(_._1.isDefined).map(_._1.get).toList
	}

	def jsonFormatFeature(feature: ProcessFeature): String = {
		val targetType = FeatureTypeDescription.of(feature)
		s"""
		   "${feature.name}" : {
	 			"type" : "${targetType.typ}",
	 			"size" : ${targetType.size},
	 			${targetType.options}
		   }
		 """
	}

	private sealed abstract class FeatureTypeDescription(val typ: String, val size: Int = 1) {
		def options: String
	}

	object FeatureTypeDescription {
		private[SpearmintConfigExporter] def of(feature: ProcessFeature): FeatureTypeDescription = {
			val range = enumRangeFor(feature)
			val enumFeatureType = new EnumFeatureType(range)
			if (range.forall(_.matches("[0-9]*"))) {
				val numericRange = range.map(_.toInt).sorted
				val (min, max) = (numericRange.head, numericRange.last)
				if (max - min == range.length + 1) new IntFeatureType(min, max) else enumFeatureType
			} else enumFeatureType
		}
	}

	private class EnumFeatureType(range: List[String]) extends FeatureTypeDescription("ENUM") {
		def options: String = s""""options" : [$optionsList]"""

		protected def optionsList: String = {
			range.map(f => "\"" + f + "\"").mkString(", ")
		}
	}

	private class IntFeatureType(min: Int, max: Int) extends FeatureTypeDescription("INT") {
		def options: String =
			s""""min" : $min,
			   	 "max" : $max
			 """
	}

	def jsonFeatureDescription(features: List[ProcessFeature]) =
		features.map(f => jsonFormatFeature(f)).mkString(",")

	def fullJson(features: List[ProcessFeature], experimentName: String, mongoPort: Int) =
		s"""
		   {
		     "language": "PYTHON",
		     "experiment-name": "$experimentName",
		     "polling-time": 1,
		     "resources": {
		       "my-machine": {
		         "scheduler": "local",
		         "max-concurrent": 10,
		         "max-finished-jobs": 10000
		       }
		     },
 		  "database" : {
		       "address" : "localhost:$mongoPort"
		     },
		     "tasks": {
		       "branin": {
		         "type": "OBJECTIVE",
		         "likelihood": "GAUSSIAN",
		         "main-file": "branin",
		         "resources": [
		           "my-machine"
		         ]
		       }
		     },
		     "variables": { ${jsonFeatureDescription(features)} }
			}
		"""

	def storeAsJson(outfile: File, features: List[ProcessFeature] = featureExpander.featuresInclClass.toList, experimentName: String = "PPLib", mongoPort: Int = 27017): Unit = {
		Some(new FileWriter(outfile)).foreach(f => {
			f.write(fullJson(features, experimentName, mongoPort))
			f.close()
		})
	}
}
