
organization := "edu.berkeley.cs"

val projDeps = condep.ChiselDependencies.dependencies(Seq(
  ("edu.berkeley.cs" %% "subproject1" % "0.1-SNAPSHOT", "subproject1.NOT"),
  ("edu.berkeley.cs" %% "subproject2" % "0.1-SNAPSHOT", "subproject2")
))

val dependentProjects = projDeps.projects

lazy val root = (project in file("."))
    .settings(
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.11.11",
      publishLocal := {},
      publish := {},
      packagedArtifacts := Map.empty,
      libraryDependencies ++= projDeps.libraries
    )
    .dependsOn(dependentProjects.map(classpathDependency(_)): _*)
    .aggregate(dependentProjects: _*)
