
organization := "edu.berkeley.cs"

val projDeps = condep.ChiselDependencies.dependencies(Seq(
  ("edu.berkeley.cs" %% "subproject1" % "0.1-SNAPSHOT", "subproject1.NOT")
))

val dependentProjects = projDeps.projects

lazy val subproject2 = (project in file("."))
  .settings(
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.11",
    libraryDependencies ++= projDeps.libraries
  )
  .dependsOn(dependentProjects.map(classpathDependency(_)): _*)
  .aggregate(dependentProjects: _*)
