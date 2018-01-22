// The following is required for sbt 0.13.xx compatible versions of scripted plugin,
//  but undefined (!) for sbt 1.0.x versions.
//ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dsbt.log.noformat=true", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false
