name := "Xmlrpc"

version := "1.0"

organization := "jvican"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= {
  val scalazVersion = "7.1.1"
  val akkaVersion = "2.3.9"
  val sprayVersion = "1.3.3"
  val scalaTestVersion = "2.2.4"
  val shapelessVersion = "2.2.0-RC4"

  Seq(
    "org.scalaz"        %% "scalaz-core"    % scalazVersion,
    "io.spray"          %% "spray-http"     % sprayVersion,
    "io.spray"          %% "spray-httpx"    % sprayVersion,
    "io.spray"          %% "spray-can"      % sprayVersion,
    "io.spray"          %% "spray-util"     % sprayVersion,
    "io.spray"          %% "spray-client"   % sprayVersion,
    "com.typesafe.akka" %%  "akka-actor"    % akkaVersion,
    "org.scalatest"     %% "scalatest"      % scalaTestVersion    % "test",
    "com.chuusai"       %% "shapeless"      % shapelessVersion
  )
}
