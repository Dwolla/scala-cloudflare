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
  scalacOptions ++= Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-Xfuture",                          // Turn on future language features.
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match",              // Pattern match may not be typesafe.
    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",             // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-value-discard",              // Warn when non-Unit expression results are unused.
//    "-Xlog-implicits",                 // normally left disabled because it's super noisy
  ) ++ (scalaBinaryVersion.value match {
    case "2.12" ⇒ Seq(
      "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
      "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
      "-Ywarn-unused:explicits",           // Warn if an explicit parameter is unused.
      "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals",              // Warn if a local definition is unused.
      "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates",            // Warn if a private member is unused.
    )
    case _ ⇒ Seq.empty
  }),
  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
  scalacOptions in Compile in Test -= "-Xfatal-warnings",
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4"),
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
