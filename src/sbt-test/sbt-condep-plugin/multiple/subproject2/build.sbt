
organization := "edu.berkeley.cs"

val projDeps = condep.Depends(
  ("subproject1", null, "edu.berkeley.cs" %% "subproject1" % "0.1-SNAPSHOT")
)

lazy val subproject2 = (project in file("."))
  .settings(
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.11",
    libraryDependencies ++= projDeps.libDeps
  )
