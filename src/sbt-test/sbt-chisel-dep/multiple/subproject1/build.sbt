
organization := "edu.berkeley.cs"

lazy val subproject1 = (project in file("."))
  .settings(
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.12.4"
  )
