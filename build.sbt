import Dependencies._

lazy val buildSettings = Seq(
  organization := "com.dwolla",
  homepage := Some(url("https://github.com/Dwolla/scala-cloudflare")),
  description := "Scala library for the Cloudflare v4 API",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  releaseVersionBump := sbtrelease.Version.Bump.Minor,
  releaseCommitMessage :=
    s"""${releaseCommitMessage.value}
        |
        |[ci skip]""".stripMargin,
  scalaVersion := "2.12.6",
  startYear := Option(2016),
  resolvers ++= Seq(
    Resolver.bintrayIvyRepo("dwolla", "maven"),
  ),
  scalacOptions += "-deprecation"
)

lazy val bintraySettings = Seq(
  bintrayVcsUrl := homepage.value.map(_.toString),
  publishMavenStyle := true,
  bintrayRepository := "maven",
  bintrayOrganization := Option("dwolla"),
  pomIncludeRepository := { _ ⇒ false },
)


lazy val dto = (project in file("dto"))
  .settings(buildSettings ++ bintraySettings: _*)
  .settings(name := "cloudflare-api-dto")

lazy val client = (project in file("client"))
  .settings(buildSettings ++ bintraySettings: _*)
  .settings(name := "cloudflare-api-client")
  .settings(libraryDependencies ++= Seq(
      scalaArm,
      json4s,
      httpComponents,
      specs2Core % Test,
      specs2Mock % Test,
      specs2Matchers % Test,
      dwollaTestUtils % Test,
    )
  )
  .dependsOn(dto)

lazy val scalaCloudflare = (project in file("."))
  .settings(buildSettings ++ noPublishSettings: _*)
  .aggregate(dto, client)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  Keys.`package` := file(""),
)