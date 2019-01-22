logLevel := Level.Warn
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.10")
addSbtPlugin("com.dwijnand" % "sbt-travisci" % "1.1.3")
addSbtPlugin("com.dwolla.sbt" % "sbt-dwolla-base" % "1.3.0")

resolvers += Resolver.bintrayIvyRepo("dwolla", "sbt-plugins")
