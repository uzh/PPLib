package ch.uzh.ifi.pdeboer.pplib.examples.optimization

/**
  * Created by pdeboer on 13/12/15.
  */

import java.io.{FileWriter, File}

import ch.uzh.ifi.pdeboer.pplib.process.entities.{ProcessFeature, SurfaceStructureFeatureExpander}

class SpearmintConfigExporter[INPUT, OUTPUT <: Comparable[OUTPUT]](featureExpander: SurfaceStructureFeatureExpander[INPUT, OUTPUT]) {
	/*
		 "X":{"type"    : "ENUM",
		  "size"    : 2,
		  "options" : ["one", "two", "three"]}
	*/

	def enumRangeFor(feature: ProcessFeature): List[String] = {
		val range = featureExpander.surfaceStructures.groupBy(s => featureExpander.featureValueAt(feature, s))

		range.map(_._1.getOrElse("None")).toList
	}

	def jsonFormatFeature(feature: ProcessFeature): String = {
		s"""
		   "${feature.name}" : {
	 			"type" : "ENUM",
	 			"size" : 2,
	 			"options" : [${enumRangeFor(feature).map(f => "\"" + f + "\"").mkString(", ")}]
		   }
		 """
	}

	def jsonFeatureDescription(features: List[ProcessFeature]) =
		features.map(f => jsonFormatFeature(f)).mkString(",")

	def fullJson(features: List[ProcessFeature]) =
		s"""
		   {
		     "language": "PYTHON",
		     "experiment-name": "noisyPPLib",
		     "polling-time": 1,
		     "resources": {
		       "my-machine": {
		         "scheduler": "local",
		         "max-concurrent": 10,
		         "max-finished-jobs": 10000
		       }
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

	def storeAsJson(outfile: File, features: List[ProcessFeature] = featureExpander.featuresInclClass.toList): Unit = {
		Some(new FileWriter(outfile)).foreach(f => {
			f.write(fullJson(features))
			f.close()
		})
	}
}
