import Build._

name := "zio-example"
organization in ThisBuild := "com.big.daddy"
version := "0.1"

resolvers += Resolver.sonatypeRepo("releases")

lazy val zioExample =
  project
    .in(file("."))
    .settings(stdSettings("zio-example"))
