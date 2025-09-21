ThisBuild / organization := "com.dwolla"
ThisBuild / description := "Scala library for the Cloudflare v4 API"
ThisBuild / homepage := Some(url("https://github.com/Dwolla/scala-cloudflare"))
ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / developers := List(
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
  )
ThisBuild / crossScalaVersions := Seq("2.12.20", "2.13.16")
ThisBuild / startYear := Option(2016)
ThisBuild / tlBaseVersion := "4.0"
ThisBuild / tlJdkRelease := Option(8)
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))
ThisBuild / tlMimaPreviousVersions ++= Set("4.0.0-M15")
ThisBuild / tlCiReleaseBranches := Seq("main")
ThisBuild / mergifyStewardConfig ~= { _.map {
  _.withAuthor("dwolla-oss-scala-steward[bot]")
    .withMergeMinors(true)
}}

lazy val dto = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("dto"))
  .settings(
    name := "cloudflare-api-dto",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic" % "0.14.14",
      "io.circe" %% "circe-literal" % "0.14.14",
      "io.circe" %% "circe-parser" % "0.14.14",
      "io.circe" %% "circe-generic-extras" % "0.14.4",
      "io.circe" %% "circe-optics" % "0.14.1",
    ),
  )
  .jsSettings(
    tlVersionIntroduced ++= Map("2.12" -> "4.0.0", "2.13" -> "4.0.0"),
  )

lazy val apiClient = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("client"))
  .settings(documentationSettings *)
  .settings(
    name := "cloudflare-api-client",
    libraryDependencies ++= {
      val http4sVersion = "0.23.30"
      Seq(
        "org.http4s" %% "http4s-dsl",
        "org.http4s" %% "http4s-circe",
        "org.http4s" %% "http4s-client",
      ).map(_ % http4sVersion) ++
        Seq(
          "co.fs2" %% "fs2-core" % "3.12.2",
          "com.dwolla" %% "fs2-utils" % "3.0.0-RC2",
          "org.typelevel" %% "cats-core" % "2.13.0",
          "org.typelevel" %% "cats-effect" % "3.6.3",
          "com.chuusai" %% "shapeless" % "2.3.13",
          "io.monix" %% "newtypes-core" % "0.3.0",
        ) ++
        Seq(
          "com.github.alexarchambault" %% "scalacheck-shapeless_1.15" % "1.3.0",
          "org.http4s" %% "http4s-server" % http4sVersion,
          "org.http4s" %% "http4s-laws" % http4sVersion,
          "org.typelevel" %% "cats-effect-laws" % "3.6.3",
          "org.specs2" %% "specs2-core" % "4.21.0",
          "org.specs2" %% "specs2-scalacheck" % "4.21.0",
          "org.specs2" %% "specs2-cats" % "4.21.0",
          "org.typelevel" %% "cats-effect-testing-specs2" % "1.7.0",
        ).map(_ % Test)
    },
  )
  .dependsOn(dto)
  .jsSettings(
    tlVersionIntroduced ++= Map("2.12" -> "4.0.0", "2.13" -> "4.0.0"),
  )

lazy val scalaCloudflare = tlCrossRootProject.aggregate(
  dto,
  apiClient,
)

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
