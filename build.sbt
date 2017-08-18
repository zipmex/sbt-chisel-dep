sbtPlugin := true

organization := "edu.berkeley.cs"

name := "sbt-condep-plugin"

version := "1.3-SNAPSHOT"

//crossSbtVersions := Vector("0.13.15", "1.0.0-RC3")

publishMavenStyle := true

publishTo := Some(Resolver.file("Local", file("gh-pages") / "maven" asFile)(
  Patterns(true, Resolver.mavenStyleBasePattern)))

ScriptedPlugin.scriptedSettings
scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dsbt.log.noformat=true", "-Dplugin.version=" + version.value)
}
scriptedBufferLog := false
