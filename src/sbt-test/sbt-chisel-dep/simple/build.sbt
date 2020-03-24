
organization := "edu.berkeley.cs"

lazy val root = (project in file("."))
  .settings(
    version := "0.1",
    scalaVersion := "2.12.4",
    assemblyJarName in assembly := "foo.jar"
  )
