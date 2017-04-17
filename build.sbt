lazy val buildSettings = Seq(
  organization := "com.dwolla",
  name := "cloudflare-api-client",
  homepage := Some(url("https://github.com/Dwolla/scala-cloudflare")),
  description := "Scala library for the Cloudflare v4 API",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  releaseVersionBump := sbtrelease.Version.Bump.Minor,
  scalaVersion := "2.12.1",
  startYear := Option(2016),
  resolvers ++= Seq(
    Resolver.bintrayIvyRepo("dwolla", "maven")
  ),
  libraryDependencies ++= {
    val specs2Version = "3.8.6"
    val json4sVersion = "3.5.1"
    Seq(
      "com.jsuereth" %% "scala-arm" % "2.0",
      "org.json4s" %% "json4s-native" % json4sVersion,
      "org.apache.httpcomponents" % "httpclient" % "4.5.2",
      "org.specs2" %% "specs2-core" % specs2Version % Test,
      "org.specs2" %% "specs2-mock" % specs2Version % Test,
      "org.specs2" %% "specs2-matcher-extra" % specs2Version % Test,
      "com.dwolla" %% "testutils" % "1.4.0" % Test
    )
  },
  scalacOptions += "-deprecation"
)

lazy val bintraySettings = Seq(
  bintrayVcsUrl := homepage.value.map(_.toString),
  publishMavenStyle := false,
  bintrayRepository := "maven",
  bintrayOrganization := Option("dwolla"),
  pomIncludeRepository := { _ ⇒ false }
)

lazy val root = (project in file("."))
  .settings(buildSettings ++ bintraySettings: _*)
