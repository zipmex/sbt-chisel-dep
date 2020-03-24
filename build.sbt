sbtPlugin := true

organization := "edu.berkeley.cs"

name := "sbt-chisel-dep"

version := "1.3-SNAPSHOT"

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

// Building cross 0.13 and 1.0 versions is too complicated due to conflicting definitions.
//crossSbtVersions := Vector("0.13.16", "1.0.4")

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

pomExtra :=
  <url>http://chisel.eecs.berkeley.edu/</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
    <scm>
      <url>https://github.com/ucb-bar/sbt-chisel-dep.git</url>
      <connection>scm:git:github.com/ucb-bar/sbt-chisel-dep.git</connection>
    </scm>
    <developers>
      <developer>
        <id>ucbjrl</id>
        <name>Jim Lawson</name>
      </developer>
    </developers>
