package controllers.project

import java.nio.file.{Files, Path}
import javax.inject.Inject

import controllers.BaseController
import db.ModelService
import discourse.OreDiscourseApi
import form.OreForms
import ore.permission.{EditSettings, HideProjects}
import ore.project.FlagReasons
import ore.project.factory.ProjectFactory
import ore.project.io.InvalidPluginFileException
import ore.user.MembershipDossier._
import ore.{OreConfig, OreEnv, StatTracker}
import org.apache.commons.io.FileUtils
import org.spongepowered.play.security.SingleSignOnConsumer
import play.api.i18n.MessagesApi
import play.api.mvc._
import util.StringUtils._
import views.html.{projects => views}

/**
  * Controller for handling Project related actions.
  */
class Projects @Inject()(stats: StatTracker,
                         forms: OreForms,
                         factory: ProjectFactory,
                         implicit override val sso: SingleSignOnConsumer,
                         implicit val forums: OreDiscourseApi,
                         implicit override val messagesApi: MessagesApi,
                         implicit override val config: OreConfig,
                         implicit override val service: ModelService)
                         extends BaseController {

  private val self = controllers.project.routes.Projects

  private def SettingsEditAction(author: String, slug: String)
  = AuthedProjectAction(author, slug, requireUnlock = true) andThen ProjectPermissionAction(EditSettings)

  /**
    * Displays the "create project" page.
    *
    * @return Create project view
    */
  def showCreator() = UserLock() { implicit request =>
    Ok(views.create(None))
  }

  /**
    * Uploads a Project's first plugin file for further processing.
    *
    * @return Result
    */
  def upload() = UserLock() { implicit request =>
    request.body.asMultipartFormData.get.file("pluginFile") match {
      case None =>
        Redirect(self.showCreator()).withError("error.noFile")
      case Some(tmpFile) =>
        // Start a new pending project
        val user = request.user
        this.factory.getUploadError(user) match {
          case None =>
            try {
              val plugin = this.factory.processPluginFile(tmpFile.ref, tmpFile.filename, user)
              val project = this.factory.startProject(plugin)
              project.cache()
              val model = project.underlying
              Redirect(self.showCreatorWithMeta(model.ownerName, model.slug))
            } catch {
              case e: InvalidPluginFileException =>
                Redirect(self.showCreator()).withError("error.project.invalidPluginFile")
            }
          case Some(error) =>
            Redirect(self.showCreator()).withError(error)
        }
    }
  }

  /**
    * Displays the "create project" page with uploaded plugin meta data.
    *
    * @param author Author of plugin
    * @param slug   Project slug
    * @return Create project view
    */
  def showCreatorWithMeta(author: String, slug: String) = UserLock() { implicit request =>
    this.factory.getPendingProject(author, slug) match {
      case None =>
        Redirect(self.showCreator())
      case Some(pending) =>
        Ok(views.create(Some(pending)))
    }
  }

  /**
    * Shows the members invitation page during Project creation.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         View of members config
    */
  def showInvitationForm(author: String, slug: String) = UserLock() { implicit request =>
    this.factory.getPendingProject(author, slug) match {
      case None =>
        Redirect(self.showCreator())
      case Some(pendingProject) =>
        pendingProject.settings.save(pendingProject.underlying, this.forms.ProjectSave.bindFromRequest().get)
        Ok(views.invite(pendingProject))
    }
  }

  /**
    * Continues on to the second step of Project creation where the user
    * publishes their Project.
    *
    * @param author Author of project
    * @param slug   Project slug
    * @return Redirection to project page if successful
    */
  def showFirstVersionCreator(author: String, slug: String) = UserLock() { implicit request =>
    this.factory.getPendingProject(request.user.name, slug) match {
      case None =>
        Redirect(self.showCreator())
      case Some(pendingProject) =>
        pendingProject.roles = this.forms.ProjectMemberRoles.bindFromRequest.get.build()
        val pendingVersion = pendingProject.pendingVersion
        Redirect(routes.Versions.showCreatorWithMeta(author, slug, pendingVersion.underlying.versionString))
    }
  }

  /**
    * Displays the Project with the specified author and name.
    *
    * @param author Owner of project
    * @param slug   Project slug
    * @return View of project
    */
  def show(author: String, slug: String) = ProjectAction(author, slug) { implicit request =>
    val project = request.project
    this.stats.projectViewed(implicit request => Ok(views.pages.view(project, project.homePage)))
  }

  /**
    * Shortcut for navigating to a project.
    *
    * @param pluginId Project pluginId
    * @return Redirect to project page.
    */
  def showProjectById(pluginId: String) = Action { implicit request =>
    this.projects.withPluginId(pluginId) match {
      case None => NotFound
      case Some(project) => Redirect(self.show(project.ownerName, project.slug))
    }
  }

  /**
    * Displays the "discussion" tab within a Project view.
    *
    * @param author Owner of project
    * @param slug   Project slug
    * @return View of project
    */
  def showDiscussion(author: String, slug: String) = ProjectAction(author, slug) { implicit request =>
    this.stats.projectViewed(implicit request => Ok(views.discuss(request.project)))
  }

  /**
    * Posts a new discussion reply to the forums.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       View of discussion with new post
    */
  def postDiscussionReply(author: String, slug: String) = AuthedProjectAction(author, slug) { implicit request =>
    this.forms.ProjectReply.bindFromRequest.fold(
      hasErrors =>
        Redirect(self.showDiscussion(author, slug)).withError(hasErrors.errors.head.message),
      content => {
        val project = request.project
        if (project.topicId == -1)
          BadRequest
        else {
          // Do forum post and display errors to user if any
          val errors = this.forums.await(this.forums.postDiscussionReply(project, request.user, content))
          var result = Redirect(self.showDiscussion(author, slug))
          if (errors.nonEmpty)
            result = result.withError(errors.head)
          result
        }
      }
    )
  }

  /**
    * Redirect's to the project's issue tracker if any.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Issue tracker
    */
  def showIssues(author: String, slug: String) = ProjectAction(author, slug) { implicit request =>
    request.project.settings.issues match {
      case None => NotFound
      case Some(link) => Redirect(link)
    }
  }

  /**
    * Redirect's to the project's source code if any.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Source code
    */
  def showSource(author: String, slug: String) = ProjectAction(author, slug) { implicit request =>
    request.project.settings.source match {
      case None => NotFound
      case Some(link) => Redirect(link)
    }
  }

  /**
    * Shows either a customly uploaded icon for a [[models.project.Project]]
    * or the owner's avatar if there is none.
    *
    * @param author Project owner
    * @param slug Project slug
    * @return Project icon
    */
  def showIcon(author: String, slug: String) = ProjectAction(author, slug) { implicit request =>
    val project = request.project
    this.projects.fileManager.getIconPath(project) match {
      case None =>
        project.owner.user.avatarUrl.map(Redirect(_)).getOrElse(NotFound)
      case Some(iconPath) =>
        showImage(iconPath)
    }
  }

  private def showImage(path: Path) = Ok(FileUtils.readFileToByteArray(path.toFile)).as("image/jpeg")

  /**
    * Submits a flag on the specified project for further review.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       View of project
    */
  def flag(author: String, slug: String) = AuthedProjectAction(author, slug) { implicit request =>
    val user = request.user
    val project = request.project
    if (user.hasUnresolvedFlagFor(project)) {
      // One flag per project, per user at a time
      BadRequest
    } else {
      val reason = FlagReasons(this.forms.ProjectFlag.bindFromRequest.get)
      project.flagFor(user, reason)
      Redirect(self.show(author, slug)).flashing("reported" -> "true")
    }
  }

  /**
    * Sets whether a [[models.user.User]] is watching a project.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @param watching True if watching
    * @return         Ok
    */
  def setWatching(author: String, slug: String, watching: Boolean) = {
    AuthedProjectAction(author, slug) { implicit request =>
      request.user.setWatching(request.project, watching)
      Ok
    }
  }

  /**
    * Sets the "starred" status of a Project for the current user.
    *
    * @param author  Project owner
    * @param slug    Project slug
    * @param starred True if should set to starred
    * @return Result code
    */
  def setStarred(author: String, slug: String, starred: Boolean) = {
    AuthedProjectAction(author, slug) { implicit request =>
      if (request.project.ownerId != request.user.userId) {
        request.project.setStarredBy(request.user, starred)
        Ok
      } else {
        BadRequest
      }
    }
  }

  /**
    * Sets the status of a pending Project invite for the current user.
    *
    * @param id     Invite ID
    * @param status Invite status
    * @return       NotFound if invite doesn't exist, Ok otherwise
    */
  def setInviteStatus(id: Int, status: String) = Authenticated { implicit request =>
    val user = request.user
    user.projectRoles.get(id) match {
      case None =>
        NotFound
      case Some(role) =>
        val dossier = role.project.memberships
        status match {
          case STATUS_DECLINE =>
            dossier.removeRole(role)
            Ok
          case STATUS_ACCEPT =>
            role.setAccepted(true)
            Ok
          case STATUS_UNACCEPT =>
            role.setAccepted(false)
            Ok
          case _ =>
            BadRequest
        }
    }
  }

  /**
    * Shows the project manage or "settings" pane.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Project manage
    */
  def showSettings(author: String, slug: String) = SettingsEditAction(author, slug) { implicit request =>
    Ok(views.settings(request.project))
  }

  /**
    * Uploads a new icon to be saved for the specified [[models.project.Project]].
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       Ok or redirection if no file
    */
  def uploadIcon(author: String, slug: String) = SettingsEditAction(author, slug) { implicit request =>
    request.body.asMultipartFormData.get.file("icon") match {
      case None =>
        Redirect(self.showSettings(author, slug)).withError("error.noFile")
      case Some(tmpFile) =>
        val project = request.project
        val pendingDir = this.projects.fileManager.getPendingIconDir(project.ownerName, project.name)
        if (Files.notExists(pendingDir))
          Files.createDirectories(pendingDir)
        FileUtils.cleanDirectory(pendingDir.toFile)
        tmpFile.ref.moveTo(pendingDir.resolve(tmpFile.filename).toFile, replace = true)
        Ok
    }
  }

  /**
    * Resets the specified Project's icon to the default user avatar.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       Ok
    */
  def resetIcon(author: String, slug: String) = SettingsEditAction(author, slug) { implicit request =>
    val project = request.project
    FileUtils.deleteDirectory(this.projects.fileManager.getIconsDir(project.ownerName, project.name).toFile)
    Ok
  }

  /**
    * Displays the specified [[models.project.Project]]'s current pending
    * icon, if any.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       Pending icon
    */
  def showPendingIcon(author: String, slug: String) = ProjectAction(author, slug) { implicit request =>
    val project = request.project
    this.projects.fileManager.getPendingIconPath(project) match {
      case None => NotFound
      case Some(path) => showImage(path)
    }
  }

  /**
    * Removes a [[ore.project.ProjectMember]] from the specified project.
    *
    * @param author Project owner
    * @param slug   Project slug
    */
  def removeMember(author: String, slug: String) = SettingsEditAction(author, slug) { implicit request =>
    this.users.withName(this.forms.ProjectMemberRemove.bindFromRequest.get.trim) match {
      case None =>
        BadRequest
      case Some(user) =>
        request.project.memberships.removeMember(user)
        Redirect(self.showSettings(author, slug))
    }
  }

  /**
    * Saves the specified Project from the settings manage.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return View of project
    */
  def save(author: String, slug: String) = SettingsEditAction(author, slug) { implicit request =>
    val project = request.project
    project.settings.save(project, this.forms.ProjectSave.bindFromRequest().get)
    Redirect(self.show(author, slug))
  }

  /**
    * Renames the specified project.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Project homepage
    */
  def rename(author: String, slug: String) = SettingsEditAction(author, slug) { implicit request =>
    val newName = compact(this.forms.ProjectRename.bindFromRequest.get)
    if (!projects.isNamespaceAvailable(author, slugify(newName))) {
      Redirect(self.showSettings(author, slug)).withError("error.nameUnavailable")
    } else {
      val project = request.project
      this.projects.rename(project, newName)
      Redirect(self.show(author, project.slug))
    }
  }

  /**
    * Sets the visible state of the specified Project.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @param visible  Project visibility
    * @return         Ok
    */
  def setVisible(author: String, slug: String, visible: Boolean) = {
    (AuthedProjectAction(author, slug, requireUnlock = true)
      andThen ProjectPermissionAction(HideProjects)) { implicit request =>
      request.project.setVisible(visible)
      Ok
    }
  }

  /**
    * Irreversibly deletes the specified project.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Home page
    */
  def delete(author: String, slug: String) = SettingsEditAction(author, slug) { implicit request =>
    val project = request.project
    this.projects.delete(project)
    Redirect(ShowHome).withSuccess(this.messagesApi("project.deleted", project.name))
  }

}
