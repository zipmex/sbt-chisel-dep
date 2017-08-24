//
// $Id$

package condep

import sbt._
import Keys._

object ChiselProjectDependenciesPlugin extends AutoPlugin {
  // Common Chisel project settings.
  // These may be overridden (or augmented) on an individual project basis.
  lazy val chiselProjectSettings: Seq[Def.Setting[_]] = Seq(
    organization := "edu.berkeley.cs",
    scalaVersion := "2.11.11",

    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases")
    ),

    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
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
}

object ChiselDependencies {
  // The argument type for dependency specification.
  // In the general case, clients will furnish two arguments:
  //  - the ModuleID for the library form of the dependency,
  //  - the top-level directory containing the sbt project form of the dependency.
  type ProjectOrModuleTuple2 = (/* library dependency */ ModuleID, /* project directory */ String)
  // In rare cases, where the project is one of many in the same build.sbt file, an additional argument is required:
  //  - the project name in the subproject build.sbt file.
  type ProjectOrModuleTuple3 = (/* library dependency */ ModuleID, /* project directory */ String, /* subproject in project's sbt file */ Option[String])
  // In even rarer cases, only the library version of the dependency is available, and no project argument is required.
  type ProjectOrModuleTuple1 = (/* library dependency */ ModuleID)
  // Internally, we use a small case class to embody the dependency definition.
  private[ChiselDependencies] case class ProjectOrModule(library: ModuleID, buildURI: String, subProj: Option[String] = None) {
    def this(t: ProjectOrModuleTuple2) {
      this( t._1, t._2)
    }
    def this(t: ProjectOrModuleTuple3) {
      this( t._1, t._2, t._3)
    }
    def this(t: ProjectOrModuleTuple1) {
      this( t, "")
    }
  }
  //  and some implicit conversion methods for the tuples used in the client argument list.
  implicit def tuple3ToProjectOrModule(a: ProjectOrModuleTuple3): ProjectOrModule = {
    new ProjectOrModule(a)
  }
  implicit def tuple2ToProjectOrModule(a: ProjectOrModuleTuple2): ProjectOrModule = {
    new ProjectOrModule(a)
  }
  implicit def tuple1ToProjectOrModule(a: ProjectOrModuleTuple1): ProjectOrModule = {
    new ProjectOrModule(a)
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
  class Depends (val deps: Seq[ProjectOrModule])
  {
    /**
      * Returns a sequence of all dependent ModuleIDs
      *  for which a top-level subproject directory does not exist.
      * Suitable for use as a Seq of libraryDependencies.
      */
    def libraries: Seq[ModuleID] = deps collect {
      case dep: ProjectOrModule if !packageProjectsMap.contains(dep.buildURI) => dep.library
    }

    /**
      * Return a sequence of all dependent ProjectReferences
      *  for which a top-level subproject directory exists.
      * Suitable for use as an argument to aggregate() or dependsOn() (after wrapping with a classpathDependency()).
      */
    def projects: Seq[ProjectReference] = deps collect {
      case dep: ProjectOrModule if packageProjectsMap.contains(dep.buildURI) => packageProjectsMap(dep.buildURI)
    }
  }

  def dependencies (deps: Seq[ProjectOrModule]): Depends = {
    // Memorize the "top-level" directory, assuming our first call is from the root project.
    // We may have a broken top-level project that doesn't define the dependencies,
    //  in which case we may be in the first subproject to do so.
    // Supposedly the possibly dependent projects won't be found, so we'll pull in the Ivy libraries.
    lazy val rootDir = file(".").getCanonicalFile

    // Return an sbt ProjectReference for a project dependency
    def symproj(dep: ProjectOrModule): ProjectReference = {
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
