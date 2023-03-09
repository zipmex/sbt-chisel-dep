sbt-chisel-dep
=================

This plugin (forked from ucb-bar/sbt-chisel-dep, forked from samskivert/sbt-condep-plugin) adds the ability to have conditional direct dependencies on other projects.
By default, conditional dependencies will reference an Ivy (or Maven)
artifact, but if you create an appropriately named symlink (or directory)
in your top-level project directory for the depended-upon project,
a direct (SBT) dependency will be established on that project.
This allows automatic rebuilding of depended-upon
projects when running targets in the depending project,
and provides a magical
"things just get recompiled when they need to" experience,
like one gets with IDEs.

Usage
-----

Add the plugin to your `project/plugins.sbt`:
    
    addSbtPlugin("com.zipmex" % "sbt-chisel-dep" % "2.0")

In your `build.sbt` file, declare and use your conditional
dependencies like so:

    val projDeps = chisel.dependencies(Seq(
      ("edu.berkeley.cs" %% "firrtl" % "1.0-SNAPSHOT", "firrtl"),
      ("edu.berkeley.cs" %% "chisel3" % "3.0-SNAPSHOT", "chisel3")
    ))
    
    val dependentProjects = projDeps.projects

    lazy val myproj = (project in file("."))
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
    

The arguments to `chisel.dependencies()` are a `Seq()` of tuples, where the
first element of the tuple is the `ModuleID` to use for a library dependency
on the subproject, and the second is the name of a subdirectory containing
the dependency as an sbt (sub)project.

By default, your project will use the Ivy/Maven artifacts (libraries) as dependencies,
but if you create `firrtl` and/or `chisel3` symlinks (or directories) in your
top-level project directory, it will use direct SBT subproject dependencies.

You can confirm that the direct dependencies are working by entering the
following into the SBT console:

    show myproj/project-dependencies

It will output something like the following:

    [info] List(edu.berkeley.cs:firrtl:1.1-SNAPSHOT, edu.berkeley.cs:chisel3:3.1-SNAPSHOT)

Note that the versions in that list will be the versions defined in the SBT
build for those projects, which could differ from the versions you specify for
your Ivy/Maven artifacts.

If your subpojects themselves have dependencies, they will automatically
use any SBT subprojects found by the top-level project,
assuming they use the `sbt-chisel-dep` plugin and contain a similar call
to `chisel.dependencies()` in their `build.sbt`

The plugin also defines two sets of `settings`:
 - `chiselProjectSettings` default edu.berkeley.cs definitions for Chisel BIG4 projects (this should only be used by Berkeley projects),
 - `chiselBuildInfoSettings` reasonable settings for the `BuildInfoPlugin`

Check the [source code](https://github.com/ucb-bar/sbt-chisel-dep/blob/master/src/main/scala/chisel/Depends.scala)
for the values of these settings.

To add either of these to your build, add either (or both) of the following to your `build.sbt`:

    ChiselProjectDependenciesPlugin.chiselProjectSettings

and/or

    enablePlugins(BuildInfoPlugin)

    ChiselProjectDependenciesPlugin.chiselBuildInfoSettings

If you have multiple projects in your `build.sbt` file and you want to add
these selectively:

    lazy val myproj = (project in file(".")).
    ...
    settings(ChiselProjectDependenciesPlugin.chiselProjectSettings: _*).
    ...

and/or

    lazy val yourproj = (project in file(".")).
    ...
    enablePlugins(BuildInfoPlugin).
    settings(ChiselProjectDependenciesPlugin.chiselBuildInfoSettings: _*).
    ...

License
-------

This code is released under the New BSD License. See [LICENSE] for details.

[LICENSE]: https://github.com/ucb-bar/sbt-chisel-dep/blob/master/LICENSE
