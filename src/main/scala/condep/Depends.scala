//
// $Id$

package condep

import sbt._
import sbt.Logger
import Keys._

object ChiselProjectDependenciesPlugin extends AutoPlugin {
  // The top/root directory for the project.
  var topDir:  Option[File] = None
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
  private[ChiselDependencies] case class ProjectOrModule(buildURI: String, subProj: Option[String], library: ModuleID) {
    def this(t: ProjectOrModuleTuple) {
      this(t._1, t._2, t._3)
    }
    var projectReference: Option[ProjectReference] = None
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
  class Depends (var useProject: (String) => Boolean, rootDir: Option[File], val deps: Seq[ProjectOrModule])
  {
    /**
      * Returns a list of all dependencies that could not be resolved via their local symlink.
      */
    def libDeps: Seq[ModuleID] = deps collect {
      case dep: ProjectOrModule if !useProject(dep.buildURI) => dep.library
    }

    private[ChiselDependencies] def symproj(dep: ProjectOrModule) = {
      val dir: File = rootDir match {
        case None => file(dep.buildURI)
        case Some(dir: File) => dir / dep.buildURI
      }
      if (dep.subProj.isEmpty) {
        RootProject(dir)
      } else {
        ProjectRef(dir, dep.subProj.get)
      }
    }

    private[ChiselDependencies] def saveRef(dep: ProjectOrModule): ProjectReference = {
      if (dep.projectReference.isEmpty) {
        dep.projectReference = Some(symproj(dep))
      }
      dep.projectReference.get
    }

    /**
      * Return a sequence of projects we intend to build/depend on,
      *   suitable for use as an argument to aggregate() or dependsOn.
      */
    def projects: Seq[ProjectReference] = deps collect { case dep: ProjectOrModule if useProject(dep.buildURI) => saveRef(dep)
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
    // Is this the root project (i.e., have we been here before)?
    // We need a better way to determine this.
    // We may have a broken top-level project that doesn't define the dependencies,
    //  in which case we may be in the first subproject to do so.
    // Supposedly the possibly dependent projects won't be found, so we'll pull in the Ivy libraries.
    if (packageProjectsMap.isEmpty) {
      ChiselProjectDependenciesPlugin.topDir = Some(file(".").getCanonicalFile)
    }
//    log.debug(s"In dependencies: ${ChiselProjectDependenciesPlugin.topDir} ${deps.toString()}")
    val depends = new Depends(useProjectFunction, ChiselProjectDependenciesPlugin.topDir, deps map (new ProjectOrModule(_)))
    depends.deps.foreach { dep =>
      val id: String = dep.buildURI
      if (!packageProjectsMap.contains(id) && useProjectFunction(id)) {
//        log.debug(s"adding $id to map")
        packageProjectsMap(id) = depends.symproj(dep)
      }
    }
//    log.debug(s"packageProjectsMap: ${packageProjectsMap.toString()}")
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
