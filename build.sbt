import com.github.retronym.SbtOneJar._

organization := "pdeboer"

name := "PPLib"

version := "1.0"

scalaVersion := "2.11.2"

resolvers += "Clojars" at "https://clojars.org/repo"

libraryDependencies ++= Seq(
	"junit" % "junit" % "4.8.1" % "test",
	"net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
	"com.typesafe" % "config" % "1.2.1",
	"org.reflections" % "reflections" % "0.9.9",
	"com.novocode" % "junit-interface" % "0.8" % "test->default",
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"org.slf4j" % "slf4j-log4j12" % "1.7.5",
	"org.apache.commons" % "commons-vfs2" % "2.0",
	"org.apache.httpcomponents" % "httpclient" % "4.3.5"
)

libraryDependencies += "org.clojars.zaxtax" % "java-aws-mturk" % "1.6.2" exclude("org.apache.commons", "not-yet-commons-ssl") exclude("apache-xerces", "xercesImpl") exclude("apache-xerces", "resolver") exclude("apache-xerces", "xml-apis") exclude("velocity", "velocity") exclude("org.apache.velocity", "velocity") exclude("commons-beanutils", "commons-beanutils")

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.0-M1"

oneJarSettings

//mainClass in oneJar := Some("ch.uzh.ifi.pdeboer.pplib.Survey")
