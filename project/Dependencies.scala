import sbt._

object Dependencies {
  val specs2Core = "org.specs2" %% "specs2-core" % "4.14.0"
  val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Core.revision
  val specs2Cats = "org.specs2" %% "specs2-cats" % "4.12.12"
  val dwollaTestUtils = "com.dwolla" %% "testutils" % "2.0.0-M6"
  val catsCore = "org.typelevel" %% "cats-core" % "2.7.0"
  val catsLaws = "org.typelevel" %% "cats-core" % catsCore.revision
  val catsEffect = "org.typelevel" %% "cats-effect" % "2.5.4"
  val catsEffectLaws = "org.typelevel" %% "cats-effect-laws" % catsEffect.revision
  val fs2 = "co.fs2" %% "fs2-core" % "2.5.10"
  val dwollaFs2Utils = "com.dwolla" %% "fs2-utils" % "2.0.0-M16"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.8"

  val circeAll: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-literal",
    "io.circe" %% "circe-parser",
  ).map(_ % "0.14.1") ++ Seq(
    "io.circe" %% "circe-generic-extras" % "0.14.1",
    "io.circe" %% "circe-optics" % "0.14.1",
  )
}
