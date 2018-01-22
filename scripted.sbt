//ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dsbt.log.noformat=true", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false
