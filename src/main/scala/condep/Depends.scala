//
// $Id$

package condep

import sbt._

/**
 * Allows projects to be symlinked into the current directory for a direct dependency, or fall back
 * to obtaining the project from Maven otherwise.
 */
class Depends (testProject: Option[(String) => Boolean], deps :(String, String, ModuleID)*)
{
  val useProject: ((String) => Boolean) = testProject match {
    case Some(f: ((String) => Boolean)) => f
    case None => file(_).exists
  }
  /**
   * Adds direct dependencies to the supplied project for all dependencies that could be resolved
   * via their local symlink.
   */
  def addDeps (p :Project) = (deps collect {
    case (id, subp, dep) if (useProject(id)) => symproj(file(id), subp)
  }).foldLeft(p) { _ dependsOn _ }

  /**
   * Returns a list of all dependencies that could not be resolved via their local symlink.
   */
  def libDeps = deps collect {
    case (id, subp, dep) if (!useProject(id)) => dep
  }

  private def symproj (dir :File, subproj :String = null) =
    if (subproj == null) RootProject(dir) else ProjectRef(dir, subproj)

  /**
    * Return a sequence of projects we intend to build/depend on,
    *   suitable for use as an argument to aggregate().
    */
  def projects: Seq[ProjectReference] = deps collect {
    case (id, subp, dep) if (useProject(id)) => symproj(file(id), subp)
  }
}

object Depends
{
  def apply (deps :(String, String, ModuleID)*) = new Depends(None, deps :_*)
}
