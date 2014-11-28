organization := "pdeboer"

name := "PPLib"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
	"junit" % "junit" % "4.8.1" % "test",
	"com.typesafe" % "config" % "1.2.1",
	"org.reflections" % "reflections" % "0.9.9",
	"com.novocode" % "junit-interface" % "0.8" % "test->default",
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"org.slf4j" % "slf4j-log4j12" % "1.7.5",
	"org.apache.commons" % "commons-vfs2" % "2.0",
	"org.apache.httpcomponents" % "httpclient" % "4.3.5",
	"org.scala-lang.modules" %% "scala-xml" % "1.0.2",
	"net.sf.opencsv" % "open-csv" % "2.3"
)

/*
libraryDependencies ++= Seq(
	// other dependencies here
	"org.scalanlp" %% "breeze" % "0.10",
	// native libraries are not included by default. add this if you want them (as of 0.7)
	// native libraries greatly improve performance, but increase jar sizes.
	"org.scalanlp" %% "breeze-natives" % "0.10"
)
*/

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.0-M1"


//mainClass in assembly := Some("ch.uzh.ifi.pdeboer.pplib.Survey")

// To Skip Tests:
//test in assembly := {}
