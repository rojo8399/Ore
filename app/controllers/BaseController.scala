package controllers

import db.ModelService
import db.impl.VersionTable
import db.impl.access.{OrganizationBase, ProjectBase, UserBase}
import models.project.{Project, Version}
import ore.{OreConfig, OreEnv}
import org.spongepowered.play.security.SingleSignOnConsumer
import play.api.i18n.I18nSupport
import play.api.mvc._
import util.StringUtils._

/**
  * Represents a Secured base Controller for this application.
  */
abstract class BaseController(implicit val config: OreConfig,
                              val service: ModelService,
                              override val sso: SingleSignOnConsumer)
                              extends Controller
                                with Actions
                                with I18nSupport {

  implicit override val users: UserBase = this.service.getModelBase(classOf[UserBase])
  implicit override val projects: ProjectBase = this.service.getModelBase(classOf[ProjectBase])
  implicit override val organizations: OrganizationBase = this.service.getModelBase(classOf[OrganizationBase])

  /**
    * Executes the given function with the specified result or returns a
    * NotFound if not found.
    *
    * @param author   Project author
    * @param slug     Project slug
    * @param fn       Function to execute
    * @param request  Incoming request
    * @return         NotFound or function result
    */
  def withProject(author: String, slug: String)(fn: Project => Result)(implicit request: RequestHeader): Result
  = this.projects.withSlug(author, slug).map(fn).getOrElse(NotFound)

  /**
    * Executes the given function with the specified result or returns a
    * NotFound if not found.
    *
    * @param versionString  VersionString
    * @param fn             Function to execute
    * @param request        Incoming request
    * @param project        Project to get version from
    * @return               NotFound or function result
    */
  def withVersion(versionString: String)(fn: Version => Result)
                 (implicit request: RequestHeader, project: Project): Result
  = project.versions.find(equalsIgnoreCase[VersionTable](_.versionString, versionString)).map(fn).getOrElse(NotFound)

}
