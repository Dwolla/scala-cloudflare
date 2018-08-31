logLevel := Level.Warn
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.2")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.7")
addSbtPlugin("com.dwijnand" % "sbt-travisci" % "1.1.1")
addSbtPlugin("com.dwolla.sbt" % "sbt-dwolla-base" % "1.0.1")

resolvers += Resolver.bintrayIvyRepo("dwolla", "sbt-plugins")
