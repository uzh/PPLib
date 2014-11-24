import com.github.retronym.SbtOneJar._

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
	"org.scala-lang.modules" %% "scala-xml" % "1.0.2"
)

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.0-M1"

oneJarSettings

//mainClass in oneJar := Some("ch.uzh.ifi.pdeboer.pplib.Survey")
