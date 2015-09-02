organization := "pdeboer"

name := "pplib"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.7"

fork := true

libraryDependencies ++= Seq(
	"junit" % "junit" % "4.8.1" % "test",
	"org.mockito" % "mockito-core" % "1.10.19" % "test",
	"org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
	"com.typesafe" % "config" % "1.2.1",
	"org.reflections" % "reflections" % "0.9.9",
	"com.novocode" % "junit-interface" % "0.8" % "test->default",
	"com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
	"org.slf4j" % "slf4j-log4j12" % "1.7.5",
	"org.apache.commons" % "commons-vfs2" % "2.0",
	"org.apache.httpcomponents" % "httpclient" % "4.3.5",
	"org.scala-lang.modules" %% "scala-xml" % "1.0.2",
	"com.github.cb372" %% "scalacache-guava" % "0.6.3"
)

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.0-M1"


lazy val publishSettings = Seq(
	publishMavenStyle := true,
	publishArtifact in Test := false,
	pomIncludeRepository := { x => false },
	pomExtra := <url>https://github.com/pdeboer/PPLib/</url>
		<licenses>
			<license>
				<name>MIT License</name>
				<url>http://www.opensource.org/licenses/mit-license.php</url>
				<distribution>repo</distribution>
			</license>
		</licenses>
		<scm>
			<url>git@github.com:pdeboer/PPLib.git</url>
			<connection>scm:git:git@github.com:pdeboer/PPLib.git</connection>
		</scm>
		<developers>
			<developer>
				<id>pdeboer</id>
				<name>Patrick de Boer</name>
				<url>http://inventas-it.ch</url>
			</developer>
		</developers>
)

//mainClass in assembly := Some("ch.uzh.ifi.pdeboer.pplib.examples.singlequestion.MTurkCountWorkers")

// To Skip Tests:
//test in assembly := {}

//assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true)
