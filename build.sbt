import Dependencies._

lazy val buildSettings = Seq(
  organization := "com.dwolla",
  homepage := Some(url("https://github.com/Dwolla/scala-cloudflare")),
  description := "Scala library for the Cloudflare v4 API",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  startYear := Option(2016),
  resolvers ++= Seq(
    Resolver.bintrayRepo("dwolla", "maven"),
  ),
  scalacOptions += "-deprecation",
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
  .settings(name := "cloudflare-api-client")
  .settings(libraryDependencies ++= Seq(
      scalaArm,
      json4s,
      httpComponents,
      catsCore,
      catsEffect,
      specs2Core % Test,
      specs2Mock % Test,
      specs2Matchers % Test,
      dwollaTestUtils % Test,
    )
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
