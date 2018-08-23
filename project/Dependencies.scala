import sbt._

object Dependencies {
  val specs2Core = "org.specs2" %% "specs2-core" % "4.2.0"
  val dwollaTestUtils = "com.dwolla" %% "testutils" % "1.11.0"
  val catsCore = "org.typelevel" %% "cats-core" % "1.1.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "0.10.1"
  val fs2 = "co.fs2" %% "fs2-core" % "0.10.5"
  val dwollaFs2Utils = "com.dwolla" %% "fs2-utils" % "1.0.1"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"
}
