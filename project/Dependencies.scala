import sbt._

object Dependencies {
  val specs2Core = "org.specs2" %% "specs2-core" % "4.8.1"
  val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Core.revision
  val specs2Cats = "org.specs2" %% "specs2-cats" % specs2Core.revision
  val dwollaTestUtils = "com.dwolla" %% "testutils" % "2.0.0-M6"
  val catsCore = "org.typelevel" %% "cats-core" % "2.5.0"
  val catsLaws = "org.typelevel" %% "cats-core" % catsCore.revision
  val catsEffect = "org.typelevel" %% "cats-effect" % "2.4.1"
  val catsEffectLaws = "org.typelevel" %% "cats-effect-laws" % catsEffect.revision
  val fs2 = "co.fs2" %% "fs2-core" % "2.5.4"
  val dwollaFs2Utils = "com.dwolla" %% "fs2-utils" % "2.0.0-M11"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"

  val circeAll: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-literal",
    "io.circe" %% "circe-parser",
  ).map(_ % "0.13.0") ++ Seq(
    "io.circe" %% "circe-generic-extras" % "0.12.2",
    "io.circe" %% "circe-optics" % "0.12.0",
  )
}
