//
// $Id$

package chisel

import sbt._
import Keys._
import sbtbuildinfo._
import sbtbuildinfo.BuildInfoKeys._

object ChiselProjectDependenciesPlugin extends AutoPlugin {
  override def trigger = allRequirements
  def scalacOptionsVersion(scalaVersion: String): Seq[String] = {
    Seq() ++ {
      // If we're building with Scala > 2.11, enable the compile option
      //  switch to support our anonymous Bundle definitions:
      //  https://github.com/scala/bug/issues/10047
      CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, scalaMajor: Int)) if scalaMajor < 12 => Seq()
        case _ => Seq("-Xsource:2.11")
      }
    }
  }

  def javacOptionsVersion(scalaVersion: String): Seq[String] = {
    Seq() ++ {
      // Scala 2.12 requires Java 8. We continue to generate
      //  Java 7 compatible code for Scala 2.11
      //  for compatibility with old clients.
      CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, scalaMajor: Int)) if scalaMajor < 12 =>
          Seq("-source", "1.7", "-target", "1.7")
        case _ =>
          Seq("-source", "1.8", "-target", "1.8")
      }
    }
  }

  // Common Chisel project settings.
  // These may be overridden (or augmented) on an individual project basis.
  lazy val chiselProjectSettings: Seq[Def.Setting[_]] = Seq(
    organization := "edu.berkeley.cs",
    scalaVersion := "2.11.11",
    crossScalaVersions := Seq("2.11.11", "2.12.3"),

    scalacOptions ++= scalacOptionsVersion(scalaVersion.value),
    javacOptions ++= javacOptionsVersion(scalaVersion.value),

    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases")
    ),

    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomIncludeRepository := { x => false },

    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),

    pomExtra := <url>http://chisel.eecs.berkeley.edu/</url>
      <licenses>
        <license>
          <name>BSD-style</name>
          <url>http://www.opensource.org/licenses/bsd-license.php</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
  )

  // BuildInfo
  lazy val chiselBuildInfoSettings: Seq[Def.Setting[_]] = Seq(
    buildInfoPackage := "`" + name.value + "`",
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoUsePackageAsPath := true,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
  )

}

object dependencies {
  // The argument types for dependency specification.
  // In the general case, clients will furnish two arguments:
  //  - the ModuleID for the library form of the dependency,
  //  - the top-level directory containing the sbt project for the project form of the dependency.
  type ProjectOrLibraryTuple2 = (/* library dependency */ ModuleID, /* project directory */ String)
  // In rare cases, where the project is one of many in the same build.sbt file, an additional argument is required:
  //  - the project name in the subproject build.sbt file.
  type ProjectOrLibraryTuple3 = (/* library dependency */ ModuleID, /* project directory */ String, /* subproject in project's sbt file */ Option[String])
  // In even rarer cases, only the library version of the dependency is available, and no project argument is required,
  //  or only a project argument is available and no library version is published.
  // We signify the former (library but no project) with a project buildURI of "".

  // Internally, we use a small case class to embody the dependency definition.
  private[dependencies] case class ProjectOrLibrary (library: Option[ModuleID], buildURI: String, subProj: Option[String] = None) {
    def this(t: ProjectOrLibraryTuple2) {
      this( Some(t._1), t._2)
    }
    def this(t: ProjectOrLibraryTuple3) {
      this( Some(t._1), t._2, t._3)
    }
    def this(l: ModuleID) {
      this( Some(l), "")
    }
    def this(p: String) {
      this( None, p)
    }
  }
  //  and some implicit conversion methods for the tuples used in the client argument list.
  implicit def tuple3ToProjectOrLibrary(a: ProjectOrLibraryTuple3): ProjectOrLibrary = {
    new ProjectOrLibrary(a)
  }
  implicit def tuple2ToProjectOrLibrary(a: ProjectOrLibraryTuple2): ProjectOrLibrary = {
    new ProjectOrLibrary(a)
  }
  // The central structure defining project versions of the dependencies.
  // This is created on the first (top-level) call to define the dependencies,
  //  and read by subprojects to determine which flavor of dependency (project or library) to use.
  type PackageProjectsMap = scala.collection.mutable.Map[String, ProjectReference]
  private val packageProjectsMap: PackageProjectsMap = new scala.collection.mutable.LinkedHashMap[String, ProjectReference]()
  /**
    * Allows projects to be symlinked into the top-level directory for a direct dependency, or fall back
    * to obtaining the project from Maven otherwise.
    */
  class Depends (val deps: Seq[ProjectOrLibrary])
  {
    /**
      * Returns a sequence of all dependent ModuleIDs
      *  for which a top-level subproject directory does not exist.
      * Suitable for use as a Seq of libraryDependencies.
      */
    def libraries: Seq[ModuleID] = deps collect {
      case dep: ProjectOrLibrary if !packageProjectsMap.contains(dep.buildURI) => dep.library.get
    }

    /**
      * Return a sequence of all dependent ProjectReferences
      *  for which a top-level subproject directory exists.
      * Suitable for use as an argument to aggregate() or dependsOn() (after wrapping with a classpathDependency()).
      */
    def projects: Seq[ProjectReference] = deps collect {
      case dep: ProjectOrLibrary if packageProjectsMap.contains(dep.buildURI) => packageProjectsMap(dep.buildURI)
    }
  }

  def apply (deps: Seq[ProjectOrLibrary]): Depends = {
    // Memorize the "top-level" directory, assuming our first call is from the root project.
    // We may have a broken top-level project that doesn't define the dependencies,
    //  in which case we may be in the first subproject to do so.
    // Supposedly the possibly dependent projects won't be found, so we'll pull in the Ivy libraries.
    lazy val rootDir = file(".").getCanonicalFile

    // Return an sbt ProjectReference for a project dependency
    def symproj(dep: ProjectOrLibrary): ProjectReference = {
      val dir: File = rootDir  / dep.buildURI
      // Is this one of possibly many projects in the subproject directory?
      if (dep.subProj.isEmpty) {
        // A single project
        RootProject(dir)
      } else {
        // One of many projects
        ProjectRef(dir, dep.subProj.get)
      }
    }

    val depends = new Depends(deps)
    // For each dependency for which we can find a directory (at the top level),
    //  generate and save a ProjectReference
    depends.deps.foreach { dep =>
      val id: String = dep.buildURI
      // Don't bother with the project map if there's no directory/buildURI for this dependency.
      if (id != "" && !packageProjectsMap.contains(id) && file(id).exists) {
        packageProjectsMap(id) = symproj(dep)
      }
    }

    depends
  }
}
