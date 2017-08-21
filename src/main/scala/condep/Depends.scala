//
// $Id$

package condep

import sbt._
import Keys._

object ProjectModuleDependencies  extends AutoPlugin {
  type ProjectOrModule = (String, Option[String], ModuleID)
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
  class Depends (var useProject: (String) => Boolean, deps: Seq[ProjectOrModule])
  {
    /**
      * Returns a list of all dependencies that could not be resolved via their local symlink.
      */
    def libDeps = deps collect {
      case (id, subp, dep) if (!useProject(id)) => dep
    }

    private[ProjectModuleDependencies] def symproj (dir :File, subproj :Option[String] = null) =
      if (subproj == None) RootProject(dir) else ProjectRef(dir, subproj.get)

    /**
      * Return a sequence of projects we intend to build/depend on,
      *   suitable for use as an argument to aggregate() or dependsOn.
      */
    def projects: Seq[ProjectReference] = deps collect {
      case (id, subp, dep) if (useProject(id)) => symproj(file(id), subp)
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

  def dependencies (deps: Seq[ProjectOrModule]): Depends = {
    val depends = new Depends(useProjectFunction, deps)
    println(s"In dependencies: ${deps.toString()}")
    val verbose = 2
    if (verbose == 2) {
      for (dep <- deps) {
        println(s"dep: ${dep}")
        val (id: String, subp: Option[String], mod: ModuleID) = dep
        println(s"id: ${id}")
        if (!packageProjectsMap.contains(id) && useProjectFunction(id)) {
          println(s"adding $id to map")
          packageProjectsMap(id) = depends.symproj(file(id), subp)
        }
      }
    } else if (verbose == 1) {
      for ((id: String, subp: Option[String], mod: ModuleID) <- deps) {
        println(s"id: ${id}")
        if (!packageProjectsMap.contains(id) && useProjectFunction(id)) {
          println(s"adding $id to map")
          packageProjectsMap(id) = depends.symproj(file(id), subp)
        }
      }
    } else {
      deps.foreach { case (id: String, subp: Option[String], lib: ModuleID) =>
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
