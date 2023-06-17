import Dependencies._

inThisBuild(List(
  organization := "com.dwolla",
  description := "Scala library for the Cloudflare v4 API",
  homepage := Some(url("https://github.com/Dwolla/scala-cloudflare")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  developers := List(
    Developer(
      "bpholt",
      "Brian Holt",
      "bholt+github@dwolla.com",
      url("https://dwolla.com")
    ),
    Developer(
      "coreyjonoliver",
      "Corey Oliver",
      "corey@dwolla.com",
      url("https://dwolla.com")
    ),
  ),
  crossScalaVersions := Seq("2.13.9", "2.12.18"),
  scalaVersion := crossScalaVersions.value.head,
  startYear := Option(2016),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),

  githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"), JavaSpec.temurin("11")),
  githubWorkflowTargetTags ++= Seq("v*"),
  githubWorkflowPublishTargetBranches :=
    Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
  githubWorkflowPublish := Seq(
    WorkflowStep.Sbt(
      List("ci-release"),
      env = Map(
        "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
        "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
        "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
        "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
      )
    )
  ),
))

lazy val dto = (project in file("dto"))
  .settings(
    name := "cloudflare-api-dto",
    libraryDependencies ++= circeAll,
  )

lazy val apiClient = (project in file("client"))
  .settings(documentationSettings: _*)
  .settings(
    name := "cloudflare-api-client",
    libraryDependencies ++= {
      val http4sVersion = "0.23.21"
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
          newtypes,
        ) ++
        Seq(
          specs2Core,
          specs2Scalacheck,
          specs2Cats,
          specs2CatsEffect,
          "com.github.alexarchambault" %% "scalacheck-shapeless_1.15" % "1.3.0",
          "org.http4s" %% "http4s-server" % http4sVersion,
          "org.http4s" %% "http4s-laws" % http4sVersion,
          catsLaws,
          catsEffectLaws,
        ).map(_ % Test)
    },
    Test / scalacOptions -= {
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
  .settings(publish / skip := true)
  .aggregate(dto, apiClient)

val documentationSettings = Seq(
  autoAPIMappings := true,
  apiMappings ++= {
    // Lookup the path to jar (it's probably somewhere under ~/.ivy/cache) from computed classpath
    val classpath = (Compile / fullClasspath).value
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
