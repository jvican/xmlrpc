name := "xmlrpc"

version := "1.2-SNAPSHOT"

description := "Module that gives full compatibility with XML-RPC for Scala"

organization := "com.github.jvican"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.11.8", "2.12.1")

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= {
  val scalazVersion = "7.2.8"
  val akkaHttp = "10.0.2"
  val scalaTestVersion = "3.0.1"
  val shapelessVersion = "2.3.2"

  Seq(
    "org.scalaz"             %% "scalaz-core"    % scalazVersion,
    "com.typesafe.akka"      %% "akka-http-xml"  % akkaHttp,
    "org.scalatest"          %% "scalatest"      % scalaTestVersion    % "test",
    "com.chuusai"            %% "shapeless"      % shapelessVersion
  )
}

// Settings to publish to Sonatype
licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/MIT"))

pomExtra := <url>https://github.com/jvican/xmlrpc</url>
  <scm>
    <url>https://github.com/jvican/xmlrpc.git</url>
    <connection>scm:git:git@github.com:jvican/xmlrpc.git</connection>
  </scm>
  <developers>
    <developer>
      <id>jvican</id>
      <name>Jorge Vicente Cantero</name>
      <url>https://github.com/jvican</url>
    </developer>
  </developers>
