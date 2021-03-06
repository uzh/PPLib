organization := "pdeboer"

name := "pplib"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

fork := true

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies ++= Seq(
	"junit" % "junit" % "4.8.1" % "test",
	"org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
	"org.mockito" % "mockito-core" % "1.10.19" % "test",
	"org.scalamock" %% "scalamock-scalatest-support" % "3.2.2",
	"com.novocode" % "junit-interface" % "0.8" % "test->default",
	"com.typesafe" % "config" % "1.2.1",
	"org.reflections" % "reflections" % "0.9.9",
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"org.slf4j" % "slf4j-log4j12" % "1.7.5",
	"org.apache.commons" % "commons-vfs2" % "2.0",
	"org.apache.httpcomponents" % "httpclient" % "4.3.5",
	"org.scala-lang.modules" %% "scala-xml" % "1.0.2",
	"com.github.cb372" %% "scalacache-guava" % "0.6.3",
	"com.typesafe.play" %% "play-json" % "2.4.0-M1",
	"com.github.tototoshi" %% "scala-csv" % "1.2.2",
	"mysql" % "mysql-connector-java" % "5.1.34",
	"org.scalikejdbc" %% "scalikejdbc" % "2.3.5",
	"org.scalikejdbc" %% "scalikejdbc-config" % "2.3.5",
	"com.fasterxml.jackson.module" % "jackson-module-scala" % "2.1.2"
)

mainClass in assembly := Some("ch.uzh.ifi.pdeboer.pplib.examples.optimizationSimulation.MCOptimizeExternal")

// To Skip Tests:
test in assembly := {}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true)
