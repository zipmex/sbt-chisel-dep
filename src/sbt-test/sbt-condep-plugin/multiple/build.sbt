
organization := "edu.berkeley.cs"

val projDeps = condep.Depends(
  ("subproject1", null, "edu.berkeley.cs" %% "subproject1" % "0.1-SNAPSHOT"),
  ("subproject2", null, "edu.berkeley.cs" %% "subproject2" % "0.1-SNAPSHOT")
)

val dependentProjects = projDeps.projects

lazy val root = (project in file("."))
    .settings(
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.11.11",
      publishLocal := {},
      publish := {},
      packagedArtifacts := Map.empty,
      libraryDependencies ++= projDeps.libDeps
    )
    .dependsOn(dependentProjects.map(classpathDependency(_)): _*)
    .aggregate(dependentProjects: _*)
