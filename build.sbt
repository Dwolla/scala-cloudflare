import Dependencies._

lazy val buildSettings = Seq(
  organization := "com.dwolla",
  homepage := Some(url("https://github.com/Dwolla/scala-cloudflare")),
  description := "Scala library for the Cloudflare v4 API",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  startYear := Option(2016),
)

lazy val releaseSettings = {
  import ReleaseTransformations._
  import sbtrelease.Version.Bump._
  Seq(
    releaseVersionBump := Minor,
    releaseCrossBuild := true,
    releaseCommitMessage :=
      s"""${releaseCommitMessage.value}
         |
         |[ci skip]""".stripMargin,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      releaseStepCommandAndRemaining("testOnly -- timefactor 10"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
}

lazy val bintraySettings = Seq(
  bintrayVcsUrl := homepage.value.map(_.toString),
  publishMavenStyle := true,
  bintrayRepository := "maven",
  bintrayOrganization := Option("dwolla"),
  pomIncludeRepository := { _ ⇒ false },
)

lazy val dto = (project in file("dto"))
  .settings(buildSettings ++ bintraySettings ++ releaseSettings: _*)
  .settings(name := "cloudflare-api-dto")

lazy val client = (project in file("client"))
  .settings(buildSettings ++ bintraySettings ++ releaseSettings: _*)
  .settings(
    name := "cloudflare-api-client",
    libraryDependencies ++= {
    val http4sVersion = "0.18.15"
    Seq(
      "org.http4s" %% "http4s-dsl",
      "org.http4s" %% "http4s-circe",
      "org.http4s" %% "http4s-client",
    ).map(_ % http4sVersion) ++
      Seq(
        "io.circe" %% "circe-generic",
        "io.circe" %% "circe-generic-extras",
        "io.circe" %% "circe-literal",
        "io.circe" %% "circe-parser",
        "io.circe" %% "circe-optics",
      ).map(_ % "0.9.3") ++
      Seq(
        fs2,
        dwollaFs2Utils,
        catsCore,
        catsEffect,
        shapeless,
      ) ++
      Seq(
        specs2Core,
        dwollaTestUtils,
        "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      ).map(_ % Test)
  },
    dependencyOverrides ++= Seq(
      catsCore,
      catsEffect,
      fs2
    ),
  )
  .dependsOn(dto)

lazy val scalaCloudflare = (project in file("."))
  .settings(buildSettings ++ noPublishSettings ++ releaseSettings: _*)
  .aggregate(dto, client)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  Keys.`package` := file(""),
)
