import sbt._

object Dependencies {
  val scalaArm = "com.jsuereth" %% "scala-arm" % "2.0"
  val json4s = "org.json4s" %% "json4s-native" % "3.5.1"
  val httpComponents = "org.apache.httpcomponents" % "httpclient" % "4.5.2"
  val specs2Core = "org.specs2" %% "specs2-core" % "3.8.6"
  val specs2Mock = "org.specs2" %% "specs2-mock" % specs2Core.revision
  val specs2Matchers = "org.specs2" %% "specs2-matcher-extra" % specs2Core.revision
  val dwollaTestUtils = "com.dwolla" %% "testutils" % "1.4.0"
  val catsCore = "org.typelevel" %% "cats-core" % "1.1.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "0.10.1"
  val fs2 = "co.fs2" %% "fs2-core" % "0.10.5"
}
