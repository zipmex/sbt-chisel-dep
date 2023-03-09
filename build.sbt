enablePlugins(SbtPlugin)

val tokenSource = TokenSource.GitConfig("github.token") || TokenSource.Environment("GITHUB_TOKEN")

organization := "com.zipmex"

name := "sbt-chisel-dep"

version := "1.3-SNAPSHOT"

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

// Building cross 0.13 and 1.0 versions is too complicated due to conflicting definitions.
//crossSbtVersions := Vector("0.13.16", "1.0.4")

publishArtifact in Test := false

// Github Packages settings.
resolvers += Resolver.githubPackages("zipmex")
ThisBuild / githubOwner := "zipmex"
ThisBuild / githubRepository := "sbt-chisel-dep"
githubTokenSource := tokenSource

// Replace '+' with '-' in version string for docker tags compatibility
ThisBuild / dynverSeparator := "-"

// Remove the requirement for the v-prefix from sbt-dyn
ThisBuild / dynverVTagPrefix := false
