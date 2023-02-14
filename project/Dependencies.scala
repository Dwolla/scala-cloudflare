import sbt._

object Dependencies {
  val specs2Core = "org.specs2" %% "specs2-core" % "4.19.2"
  val specs2Scalacheck = "org.specs2" %% "specs2-scalacheck" % specs2Core.revision
  val specs2Cats = "org.specs2" %% "specs2-cats" % specs2Core.revision
  val specs2CatsEffect = "org.typelevel" %% "cats-effect-testing-specs2" % "1.5.0"
  val catsCore = "org.typelevel" %% "cats-core" % "2.9.0"
  val catsLaws = "org.typelevel" %% "cats-core" % catsCore.revision
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.4.7"
  val catsEffectLaws = "org.typelevel" %% "cats-effect-laws" % catsEffect.revision
  val fs2 = "co.fs2" %% "fs2-core" % "3.6.1"
  val dwollaFs2Utils = "com.dwolla" %% "fs2-utils" % "3.0.0-RC1"
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.10"
  val newtypes = "io.monix" %% "newtypes-core" % "0.2.3"

  val circeAll: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-literal",
    "io.circe" %% "circe-parser",
  ).map(_ % "0.14.1") ++ Seq(
    "io.circe" %% "circe-generic-extras" % "0.14.3",
    "io.circe" %% "circe-optics" % "0.14.1",
  )
}
