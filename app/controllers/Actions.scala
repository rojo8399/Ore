package controllers

import controllers.Requests._
import db.access.ModelAccess
import db.impl.access.{OrganizationBase, ProjectBase, UserBase}
import models.project.{Competition, Project}
import models.user.User
import ore.permission.scope.GlobalScope
import ore.permission.{EditSettings, HideProjects, Permission}
import org.spongepowered.play.security.SingleSignOnConsumer
import org.spongepowered.play.util.ActionHelpers
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * A set of actions used by Ore.
  */
trait Actions extends Calls with ActionHelpers {

  val users: UserBase
  val projects: ProjectBase
  val organizations: OrganizationBase
  val competitions: ModelAccess[Competition]
  val sso: SingleSignOnConsumer

  val PermsLogger = play.api.Logger("Permissions")

  val AuthTokenName = "_oretoken"

  /** Ensures a request is authenticated */
  def Authenticated = Action andThen authAction

  /** Ensures a user's account is unlocked */
  def UserLock(redirect: Call = ShowHome) = Authenticated andThen userLock(redirect)

  /** Called when a [[User]] tries to make a request they do not have permission for */
  def onUnauthorized(request: Request[_]) = {
    if (request.flash.get("noRedirect").isEmpty && this.users.current(request).isEmpty)
      Redirect(routes.Users.logIn(None, None, Some(request.path)))
    else
      Redirect(ShowHome)
  }

  /**
    * Action to perform a permission check for the current ScopedRequest and
    * given Permission.
    *
    * @param p  Permission to check
    * @tparam R Type of ScopedRequest that is being checked
    * @return   The ScopedRequest as an instance of R
    */
  def PermissionAction[R[_] <: ScopedRequest[_]](p: Permission) = new ActionRefiner[ScopedRequest, R] {

    private def log(success: Boolean, request: ScopedRequest[_]) = {
      val lang = if (success) "GRANTED" else "DENIED"
      PermsLogger.info(s"<PERMISSION $lang> ${request.user.name}@${request.path.substring(1)}")
    }

    def refine[A](request: ScopedRequest[A]) = Future.successful {
      if (!(request.user can p in request.subject)) {
        log(success = false, request)
        Left(onUnauthorized(request))
      } else {
        log(success = true, request)
        Right(request.asInstanceOf[R[A]])
      }
    }
  }

  /**
    * Retrieves, processes, and adds a [[Project]] to a request.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       Request with a project if found, NotFound otherwise.
    */
  def ProjectAction(author: String, slug: String) = Action andThen projectAction(author, slug)

  /**
    * Retrieves, processes, and adds a [[Project]] to a request.
    *
    * @param pluginId The project's unique plugin ID
    * @return         Request with a project if found, NotFound otherwise
    */
  def ProjectAction(pluginId: String) = Action andThen projectAction(pluginId)

  /**
    * Ensures a request is authenticated and retrieves, processes, and adds a
    * [[Project]] to a request.
    *
    * @param author Project owner
    * @param slug Project slug
    * @return Authenticated request with a project if found, NotFound otherwise.
    */
  def AuthedProjectAction(author: String, slug: String, requireUnlock: Boolean = false) = {
    val first = if (requireUnlock) UserLock(ShowProject(author, slug)) else Authenticated
    first andThen authedProjectAction(author, slug)
  }

  /**
    * A PermissionAction that uses an AuthedProjectRequest for the
    * ScopedRequest.
    *
    * @param p  Permission to check
    * @return   An [[AuthedProjectRequest]]
    */
  def ProjectPermissionAction(p: Permission) = PermissionAction[AuthedProjectRequest](p)

  /**
    * Retrieves an [[models.user.Organization]] and adds it to the request.
    *
    * @param organization Organization to retrieve
    * @return             Request with organization if found, NotFound otherwise
    */
  def OrganizationAction(organization: String) = Action andThen organizationAction(organization)

  /**
    * Ensures a request is authenticated and retrieves and adds a
    * [[models.user.Organization]] to the request.
    *
    * @param organization Organization to retrieve
    * @return             Authenticated request with Organization if found, NotFound otherwise
    */
  def AuthedOrganizationAction(organization: String, requireUnlock: Boolean = false) = {
    val first = if (requireUnlock) UserLock(ShowUser(organization)) else Authenticated
    first andThen authedOrganizationAction(organization)
  }

  /**
    * A PermissionAction that uses an AuthedOrganizationRequest for the
    * ScopedRequest.
    *
    * @param p  Permission to check
    * @return   [[AuthedOrganizationRequest]]
    */
  def OrganizationPermissionAction(p: Permission) = PermissionAction[AuthedOrganizationRequest](p)

  def CompetitionAction(id: Int) = Action andThen competitionAction(id)

  def AuthedCompetitionAction(id: Int) = Authenticated andThen authedCompetitionAction(id)

  /**
    * A request that ensures that a user has permission to edit a specified
    * profile.
    *
    * @param username User to check
    * @return [[AuthRequest]] if has permission
    */
  def UserAction(username: String) = Authenticated andThen userAction(username)

  /**
    * Represents an action that requires a user to reenter their password.
    *
    * @param username Username to verify
    * @param sso      Incoming SSO payload
    * @param sig      Incoming SSO signature
    * @return         None if verified, Unauthorized otherwise
    */
  def VerifiedAction(username: String, sso: Option[String], sig: Option[String])
  = UserAction(username) andThen verifiedAction(sso, sig)

  implicit final class ResultWrapper(result: Result) {

    /**
      * Adds a new session cookie to the result for the specified [[User]].
      *
      * @param user   User to create session for
      * @param maxAge Maximum session age
      * @return       Result with token
      */
    def authenticatedAs(user: User, maxAge: Int = -1) = {
      val session = Actions.this.users.createSession(user)
      val age = if (maxAge == -1) None else Some(maxAge)
      result.withCookies(Cookie(AuthTokenName, session.token, age))
    }

    /**
      * Indicates that the client's session cookie should be cleared.
      *
      * @return
      */
    def clearingSession() = result.discardingCookies(DiscardingCookie(AuthTokenName))

  }

  // Implementation

  private def userLock(redirect: Call) = new ActionFilter[AuthRequest] {
    def filter[A](request: AuthRequest[A]): Future[Option[Result]] = Future.successful {
      if (request.user.isLocked)
        Some(Redirect(redirect).flashing("error" -> "error.user.locked"))
      else
        None
    }
  }

  private def verifiedAction(sso: Option[String], sig: Option[String]) = new ActionFilter[AuthRequest] {
    def filter[A](request: AuthRequest[A]): Future[Option[Result]] = Future.successful {
      if (sso.isEmpty || sig.isEmpty)
        Some(Unauthorized)
      else {
        Actions.this.sso.authenticate(sso.get, sig.get) match {
          case None =>
            Some(Unauthorized)
          case Some(spongeUser) =>
            if (spongeUser.username.equals(request.user.username))
              None
            else
              Some(Unauthorized)
        }
      }
    }
  }

  private def userAction(username: String) = new ActionFilter[AuthRequest] {
    def filter[A](request: AuthRequest[A]): Future[Option[Result]] = Future.successful {
      Actions.this.users.withName(username).flatMap[User] { toCheck =>
        val user = request.user
        if (user.equals(toCheck) || (toCheck.isOrganization && (user can EditSettings in toCheck.toOrganization)))
          Some(user)
        else
          None
      }.map[Option[Result]](user => None).getOrElse(Some(Unauthorized))
    }
  }

  private def authAction = new ActionRefiner[Request, AuthRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, AuthRequest[A]]] = Future.successful {
      users.current(request)
        .map(AuthRequest(_, request))
        .toRight(onUnauthorized(request))
    }
  }

  private def projectAction(author: String, slug: String) = new ActionRefiner[Request, ProjectRequest] {
    def refine[A](request: Request[A])
    = Future.successful(maybeProjectRequest(request, Actions.this.projects.withSlug(author, slug)))
  }

  private def projectAction(pluginId: String) = new ActionRefiner[Request, ProjectRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, ProjectRequest[A]]]
    = Future.successful(maybeProjectRequest(request, Actions.this.projects.withPluginId(pluginId)))
  }

  private def maybeProjectRequest[A](request: Request[A], project: Option[Project]) = {
    project
      .flatMap(processProject(_, this.users.current(request)))
      .map(new ProjectRequest[A](_, request))
      .toRight(NotFound)
  }

  private def processProject(project: Project, user: Option[User]): Option[Project] = {
    if (project.isVisible || (user.isDefined && (user.get can HideProjects in GlobalScope)))
      Some(project)
    else
      None
  }

  private def authedProjectAction(author: String, slug: String)
  = new ActionRefiner[AuthRequest, AuthedProjectRequest] {
    def refine[A](request: AuthRequest[A]) = Future.successful {
      Actions.this.projects.withSlug(author, slug)
        .flatMap(processProject(_, Some(request.user)))
        .map(new AuthedProjectRequest[A](_, request))
        .toRight(NotFound)
    }
  }

  private def organizationAction(organization: String) = new ActionRefiner[Request, OrganizationRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, OrganizationRequest[A]]] = Future.successful {
      Actions.this.organizations.withName(organization)
        .map(new OrganizationRequest(_, request))
        .toRight(NotFound)
    }
  }

  private def authedOrganizationAction(organization: String)
  = new ActionRefiner[AuthRequest, AuthedOrganizationRequest] {
    def refine[A](request: AuthRequest[A]) = Future.successful {
      Actions.this.organizations.withName(organization)
        .map(new AuthedOrganizationRequest[A](_, request))
        .toRight(NotFound)
    }
  }

  private def competitionAction(id: Int) = new ActionRefiner[Request, CompetitionRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, CompetitionRequest[A]]] = Future.successful {
      Actions.this.competitions.get(id)
        .map(new CompetitionRequest(_, request))
        .toRight(NotFound)
    }
  }

  private def authedCompetitionAction(id: Int) = new ActionRefiner[AuthRequest, AuthedCompetitionRequest] {
    def refine[A](request: AuthRequest[A]): Future[Either[Result, AuthedCompetitionRequest[A]]] = Future.successful {
      Actions.this.competitions.get(id)
        .map(AuthedCompetitionRequest(_, request))
        .toRight(NotFound)
    }
  }

}
