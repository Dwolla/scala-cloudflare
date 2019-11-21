import Dependencies._

lazy val buildSettings = Seq(
  organization := "com.dwolla",
  homepage := Some(url("https://github.com/Dwolla/scala-cloudflare")),
  description := "Scala library for the Cloudflare v4 API",
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  startYear := Option(2016),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  resolvers += Resolver.bintrayRepo("dwolla", "maven"),
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
  pomIncludeRepository := { _ => false },
)

lazy val dto = (project in file("dto"))
  .settings(buildSettings ++ bintraySettings ++ releaseSettings: _*)
  .settings(
    name := "cloudflare-api-dto",
    libraryDependencies ++= circeAll,
  )

lazy val client = (project in file("client"))
  .settings(buildSettings ++ bintraySettings ++ releaseSettings ++ documentationSettings: _*)
  .settings(
    name := "cloudflare-api-client",
    libraryDependencies ++= {
      val http4sVersion = "0.21.0-M5"
      Seq(
        "org.http4s" %% "http4s-dsl",
        "org.http4s" %% "http4s-circe",
        "org.http4s" %% "http4s-client",
      ).map(_ % http4sVersion) ++
        circeAll ++
        Seq(
          fs2,
          dwollaFs2Utils,
          catsCore,
          catsEffect,
          shapeless,
        ) ++
        Seq(
          specs2Core,
          specs2Scalacheck,
          specs2Cats,
          "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3",
          dwollaTestUtils,
          "org.http4s" %% "http4s-blaze-server" % http4sVersion,
          "org.http4s" %% "http4s-testing" % http4sVersion,
          catsLaws,
          catsEffectLaws,
        ).map(_ % Test)
    },
    scalacOptions in Test -= {
      // Getting some spurious unreachable code warnings in 2.13 (see https://github.com/scala/bug/issues/11457)
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) =>
          "-Xfatal-warnings"
        case _ =>
          "I DON'T EXIST I'M WORKING AROUND NOT BEING ABLE TO CALL scalaVersion.value FROM ~="
      }
    },
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

val documentationSettings = Seq(
  autoAPIMappings := true,
  apiMappings ++= {
    // Lookup the path to jar (it's probably somewhere under ~/.ivy/cache) from computed classpath
    val classpath = (fullClasspath in Compile).value
    def findJar(name: String): File = {
      val regex = ("/" + name + "[^/]*.jar$").r
      classpath.find { jar => regex.findFirstIn(jar.data.toString).nonEmpty }.get.data // fail hard if not found
    }

    // Define external documentation paths
    Map(
      findJar("circe-generic-extra") -> url("http://circe.github.io/circe/api/io/circe/index.html"),
    )
  }
)
