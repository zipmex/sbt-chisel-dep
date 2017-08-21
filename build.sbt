sbtPlugin := true

organization := "edu.berkeley.cs"

name := "sbt-condep-plugin"

version := "1.3-SNAPSHOT"

//crossSbtVersions := Vector("0.13.15", "1.0.0-RC3")

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


ScriptedPlugin.scriptedSettings
scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dsbt.log.noformat=true", "-Dplugin.version=" + version.value)
}
scriptedBufferLog := false
