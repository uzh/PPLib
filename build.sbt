name := "PPLib"

version := "1.0"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
	"junit" % "junit" % "4.8.1" % "test",
	"net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
	"com.typesafe.play" %% "play-json" % "2.3.2",
	"com.typesafe" % "config" % "1.2.1",
	"org.reflections" % "reflections" % "0.9.9"
)