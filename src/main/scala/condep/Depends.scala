//
// $Id$

package condep

import sbt._
import Keys._

object ChiselProjectDependenciesPlugin  extends AutoPlugin {
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
  type ProjectOrModuleTuple = (String, Option[String], ModuleID)
  case class ProjectOrModule(buildURI: String, subProj: Option[String], library: ModuleID) {
    def this(t: ProjectOrModuleTuple) {
      this(t._1, t._2, t._3)
    }
  }
  type PackageProjectsMap = scala.collection.mutable.Map[String, ProjectReference]
  private val packageProjectsMap: PackageProjectsMap = new scala.collection.mutable.LinkedHashMap[String, ProjectReference]()
  /**
    * The default function to determine whether to use a project or library.
    */
  private val topUseProjectFunctionDefault: ((String) => Boolean) = file(_).exists
  private val subUseProjectFunctionDefault: ((String) => Boolean) = packageProjectsMap.contains(_)
  private var useProjectFunction: ((String) => Boolean) = topUseProjectFunctionDefault
  /**
    * Allows projects to be symlinked into the current directory for a direct dependency, or fall back
    * to obtaining the project from Maven otherwise.
    */
  class Depends (var useProject: (String) => Boolean, val deps: Seq[ProjectOrModule])
  {
    /**
      * Returns a list of all dependencies that could not be resolved via their local symlink.
      */
    def libDeps: Seq[ModuleID] = deps collect {
      case dep: ProjectOrModule if !useProject(dep.buildURI) => dep.library
    }

    private[ChiselDependencies] def symproj (dir :File, subproj :Option[String] = null) =
      if (subproj.isEmpty) RootProject(dir) else ProjectRef(dir, subproj.get)

    /**
      * Return a sequence of projects we intend to build/depend on,
      *   suitable for use as an argument to aggregate() or dependsOn.
      */
    def projects: Seq[ProjectReference] = deps collect {
      case dep: ProjectOrModule if useProject(dep.buildURI) => symproj(file(dep.buildURI), dep.subProj)
    }

    /**
      * Set useProjectFunction
      */
    def setUseProjectFunction(f: ((String) => Boolean)): ((String) => Boolean) = {
      val old = useProject
      useProject = f
      old
    }
  }

  def dependencies (deps: Seq[ProjectOrModuleTuple]): Depends = {
    val depends = new Depends(useProjectFunction, deps map (new ProjectOrModule(_)))
    println(s"In dependencies: ${deps.toString()}")
    val verbose = 2
    if (verbose == 2) {
      for (dep <- depends.deps) {
        println(s"dep: $dep")
        val (id: String, subp: Option[String], mod: ModuleID) = (dep.buildURI, dep.subProj, dep.library)
        println(s"id: $id")
        if (!packageProjectsMap.contains(id) && useProjectFunction(id)) {
          println(s"adding $id to map")
          packageProjectsMap(id) = depends.symproj(file(id), subp)
        }
      }
    } else if (verbose == 1) {
      for (dep <- depends.deps) {
        val (id: String, subp: Option[String], mod: ModuleID) = (dep.buildURI, dep.subProj, dep.library)
        println(s"id: $id")
        if (!packageProjectsMap.contains(id) && useProjectFunction(id)) {
          println(s"adding $id to map")
          packageProjectsMap(id) = depends.symproj(file(id), subp)
        }
      }
    } else {
      depends.deps.foreach { dep =>
        val (id: String, subp: Option[String], mod: ModuleID) = (dep.buildURI, dep.subProj, dep.library)
        if (!packageProjectsMap.contains(id) && useProjectFunction(id)) {
          packageProjectsMap(id) = depends.symproj(file(id), subp)
        }
      }
    }
    println(s"packageProjectsMap: ${packageProjectsMap.toString()}")
    depends
  }

  /**
    * Set useProjectFunction
    */
  def setUseProjectFunction(f: ((String) => Boolean)): ((String) => Boolean) = {
    val old = useProjectFunction
    useProjectFunction = f
    old
  }
}
