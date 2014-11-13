name := "PPLib"

version := "1.0"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
	"junit" % "junit" % "4.8.1" % "test",
	"net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
	"com.typesafe" % "config" % "1.2.1",
	"org.reflections" % "reflections" % "0.9.9",
	"com.novocode" % "junit-interface" % "0.8" % "test->default",
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"org.slf4j" % "slf4j-log4j12" % "1.7.5",
	"org.apache.commons" % "commons-vfs2" % "2.0"
)

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.0-M1"
