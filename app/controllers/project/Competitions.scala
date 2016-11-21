package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.Requests.AuthRequest
import db.ModelService
import form.OreForms
import models.project.Competition
import ore.OreConfig
import ore.permission.EditCompetitions
import org.spongepowered.play.security.SingleSignOnConsumer
import play.api.i18n.MessagesApi
import views.{html => views}

class Competitions @Inject()(implicit override val messagesApi: MessagesApi,
                             override val config: OreConfig,
                             override val service: ModelService,
                             override val sso: SingleSignOnConsumer,
                             forms: OreForms)
                             extends BaseController {

  private val self = routes.Competitions
  private val competitions = this.service.access[Competition](classOf[Competition])

  def EditCompetitionsAction = Authenticated andThen PermissionAction[AuthRequest](EditCompetitions)

  def showManager() = EditCompetitionsAction { implicit request =>
    Ok(views.projects.competitions.manage())
  }

  def showCreator() = EditCompetitionsAction { implicit request =>
    Ok(views.projects.competitions.create())
  }

  def create() = EditCompetitionsAction { implicit request =>
    this.forms.CompetitionCreate.bindFromRequest().fold(
      hasErrors =>
        FormError(self.showCreator(), hasErrors),
      formData => {
        this.competitions.add(new Competition(request.user, formData))
        Redirect(self.showManager())
      }
    )
  }

}
