package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.Requests.AuthRequest
import db.ModelService
import ore.OreConfig
import ore.permission.EditCompetitions
import org.spongepowered.play.security.SingleSignOnConsumer
import play.api.i18n.MessagesApi
import views.{html => views}

class Competitions @Inject()(implicit override val messagesApi: MessagesApi,
                             override val config: OreConfig,
                             override val service: ModelService,
                             override val sso: SingleSignOnConsumer)
                             extends BaseController {

  def EditCompetitionsAction = Authenticated andThen PermissionAction[AuthRequest](EditCompetitions)

  def showManager() = EditCompetitionsAction { implicit request =>
    Ok(views.projects.competitions.manage())
  }

  def showCreator() = EditCompetitionsAction { implicit request =>
    Ok(views.projects.competitions.create())
  }

}
