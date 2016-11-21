package controllers

import javax.inject.Inject

import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.access.UserBase.ORDERING_PROJECTS
import discourse.OreDiscourseApi
import form.OreForms
import models.user.role.RoleModel
import models.user.{Notification, User}
import ore.rest.OreWrites
import ore.user.notification.InviteFilters.InviteFilter
import ore.user.notification.NotificationFilters.NotificationFilter
import ore.user.notification.{InviteFilters, NotificationFilters}
import ore.user.{FakeUser, Prompts}
import ore.{OreConfig, OreEnv}
import org.spongepowered.play.mail.Mailer
import org.spongepowered.play.security.SingleSignOnConsumer
import play.api.i18n.MessagesApi
import play.api.mvc._
import util.EmailFactory
import views.{html => views}

/**
  * Controller for general user actions.
  */
class Users @Inject()(fakeUser: FakeUser,
                      forms: OreForms,
                      forums: OreDiscourseApi,
                      writes: OreWrites,
                      mailer: Mailer,
                      emails: EmailFactory,
                      implicit override val sso: SingleSignOnConsumer,
                      implicit override val messagesApi: MessagesApi,
                      implicit override val config: OreConfig,
                      implicit override val service: ModelService) extends BaseController {

  private val baseUrl = this.config.app.getString("baseUrl").get

  /**
    * Redirect to auth page for SSO authentication.
    *
    * @return Logged in page
    */
  def signUp() = Action { implicit request =>
    redirectToSso(this.sso.getSignupUrl(this.baseUrl + "/login"))
  }

  /**
    * Redirect to auth page for SSO authentication and then back here again.
    *
    * @param sso  Incoming payload from auth
    * @param sig  Incoming signature from auth
    * @return     Logged in home
    */
  def logIn(sso: Option[String], sig: Option[String], returnPath: Option[String]) = Action { implicit request =>
    if (this.fakeUser.isEnabled) {
      // Log in as fake user (debug only)
      this.config.checkDebug()
      this.users.getOrCreate(this.fakeUser)
      this.redirectBack(returnPath.getOrElse(request.path), this.fakeUser)
    } else if (sso.isEmpty || sig.isEmpty) {
      // Check if forums are available and redirect to login if so
      redirectToSso(this.sso.getLoginUrl(this.baseUrl + "/login"))
    } else {
      // Redirected from SpongeSSO, decode SSO payload and convert to Ore user
      this.sso.authenticate(sso.get, sig.get) match {
        case None =>
          Redirect(ShowHome).withError("error.loginFailed")
        case Some(spongeUser) =>
          // Complete authentication
          val user = this.users.getOrCreate(User.fromSponge(spongeUser)).pullForumData().pullSpongeData()
          this.redirectBack(request.flash.get("url").getOrElse("/"), user)
      }
    }
  }

  /**
    * Redirects the user to the auth verification page to re-enter their
    * password and then perform some action.
    *
    * @param returnPath Verified action to perform
    * @return           Redirect to verification
    */
  def verify(returnPath: Option[String]) = Authenticated { implicit request =>
    redirectToSso(this.sso.getVerifyUrl(this.baseUrl + returnPath.getOrElse("/")))
  }

  private def redirectToSso(url: String): Result = {
    if (this.sso.isAvailable)
      Redirect(url)
    else
      Redirect(ShowHome).withError("error.noLogin")
  }

  private def redirectBack(url: String, user: User)
  = Redirect(this.baseUrl + url).authenticatedAs(user, this.config.play.getInt("http.session.maxAge").get)

  /**
    * Clears the current session.
    *
    * @return Home page
    */
  def logOut(returnPath: Option[String]) = Action { implicit request =>
    Redirect(this.baseUrl + returnPath.getOrElse(request.path)).clearingSession().flashing("noRedirect" -> "true")
  }

  /**
    * Shows the User's [[models.project.Project]]s page for the user with the
    * specified username.
    *
    * @param username   Username to lookup
    * @return           View of user projects page
    */
  def showProjects(username: String, page: Option[Int]) = Action { implicit request =>
    val pageSize = this.config.users.getInt("project-page-size").get
    val p = page.getOrElse(1)
    val offset = (p - 1) * pageSize
    this.users.withName(username).map { u =>
      (u, u.projects.sorted(ordering = _.stars.desc, limit = pageSize, offset = offset))
    } map {
      case (user, projectSeq) => Ok(views.users.projects(user, projectSeq, p))
    } getOrElse {
      NotFound
    }
  }

  /**
    * Submits a change to the specified user's tagline.
    *
    * @param username   User to update
    * @return           View of user page
    */
  def saveTagline(username: String) = UserAction(username) { implicit request =>
    val tagline = this.forms.UserTagline.bindFromRequest.get.trim
    val maxLen = this.config.users.getInt("max-tagline-len").get
    val user = request.user
    if (tagline.length > maxLen) {
      Redirect(ShowUser(user)).flashing("error" -> this.messagesApi("error.tagline.tooLong", maxLen))
    } else {
      user.tagline = tagline
      Redirect(ShowUser(user))
    }
  }

  /**
    * Attempts to save a submitted PGP Public Key to the specified User
    * profile.
    *
    * @param username User to save key to
    * @return JSON response
    */
  def savePgpPublicKey(username: String) = UserAction(username) { implicit request =>
    this.forms.UserPgpPubKey.bindFromRequest.fold(
      hasErrors =>
        Redirect(ShowUser(username)).withError(hasErrors.errors.head.message),
      keySubmission => {
        import writes._
        val keyInfo = keySubmission.info
        val user = request.user
        // Validate email
        user.email match {
          case None =>
            Redirect(ShowUser(username)).withError("error.pgp.noEmail")
          case Some(email) =>
            if (!email.equals(keyInfo.email))
              Redirect(ShowUser(username)).withError("error.pgp.invalidEmail")
            else {
              user.pgpPubKey = keyInfo.raw
              if (user.lastPgpPubKeyUpdate.isDefined)
                user.lastPgpPubKeyUpdate = this.service.theTime // Not set the first time

              // Send email notification
              this.mailer.push(this.emails.create(user, this.emails.PgpUpdated))

              Redirect(ShowUser(username)).flashing("pgp-updated" -> "true")
            }
        }
      }
    )
  }

  /**
    * Deletes the specified [[User]]'s PGP public key if it exists.
    *
    * @param username Username to delete key for
    * @return Ok if deleted, bad request if didn't exist
    */
  def deletePgpPublicKey(username: String, sso: Option[String], sig: Option[String]) = {
    VerifiedAction(username, sso, sig) { implicit request =>
      val user = request.user
      if (user.pgpPubKey.isEmpty)
        BadRequest
      else {
        user.pgpPubKey = null
        user.lastPgpPubKeyUpdate = this.service.theTime
        Redirect(ShowUser(username)).flashing("pgp-updated" -> "true")
      }
    }
  }

  /**
    * Sets the "locked" status of a User.
    *
    * @param username User to set status of
    * @param locked   True if user is locked
    * @return         Redirection to user page
    */
  def setLocked(username: String, locked: Boolean, sso: Option[String], sig: Option[String]) = {
    VerifiedAction(username, sso, sig) { implicit request =>
      val user = request.user
      user.setLocked(locked)
      if (!locked)
        this.mailer.push(this.emails.create(user, this.emails.AccountUnlocked))
      Redirect(ShowUser(username))
    }
  }

  /**
    * Shows a list of [[models.user.User]]s that have created a
    * [[models.project.Project]].
    */
  def showAuthors(sort: Option[String], page: Option[Int]) = Action { implicit request =>
    val ordering = sort.getOrElse(ORDERING_PROJECTS)
    val p = page.getOrElse(1)
    Ok(views.users.authors(this.users.getAuthors(ordering, p), ordering, p))
  }

  /**
    * Displays the current user's unread notifications.
    *
    * @return Unread notifications
    */
  def showNotifications(notificationFilter: Option[String], inviteFilter: Option[String]) = {
    Authenticated { implicit request =>
      val user = request.user

      // Get visible notifications
      val nFilter: NotificationFilter = notificationFilter
        .map(str => NotificationFilters.values
          .find(_.name.equalsIgnoreCase(str))
          .getOrElse(NotificationFilters.Unread))
        .getOrElse(NotificationFilters.Unread)
      val notifications: Seq[Notification] = nFilter(user.notifications)

      // Get visible invites
      val iFilter: InviteFilter = inviteFilter
        .map(str => InviteFilters.values
          .find(_.name.equalsIgnoreCase(str))
          .getOrElse(InviteFilters.All))
        .getOrElse(InviteFilters.All)
      val invites: Seq[RoleModel] = iFilter(user)

      Ok(views.users.notifications(
        notifications,
        invites,
        nFilter, iFilter))
    }
  }

  /**
    * Marks a [[models.user.User]]'s notification as read.
    *
    * @param id Notification ID
    * @return   Ok if marked as read, NotFound if notification does not exist
    */
  def markNotificationRead(id: Int) = Authenticated { implicit request =>
    request.user.notifications.get(id) match {
      case None =>
        NotFound
      case Some(notification) =>
        notification.setRead(read = true)
        Ok
    }
  }

  /**
    * Marks a [[ore.user.Prompts.Prompt]] as read for the authenticated
    * [[models.user.User]].
    *
    * @param id Prompt ID
    * @return   Ok if successful
    */
  def markPromptRead(id: Int) = Authenticated { implicit request =>
    Prompts.values.find(_.id == id) match {
      case None =>
        BadRequest
      case Some(prompt) =>
        request.user.markPromptRead(prompt)
        Ok
    }
  }

}
