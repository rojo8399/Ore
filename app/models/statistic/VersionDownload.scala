package models.statistic

import java.sql.Timestamp

import com.github.tminglei.slickpg.InetString
import db.model.Model
import db.model.annotation.{Bind, BindingsGenerator}
import models.project.Version
import models.user.User
import ore.Statistics
import play.api.mvc.RequestHeader

import scala.annotation.meta.field

/**
  * Represents a unique download on a Project Version.
  *
  * @param id         Unique ID of entry
  * @param createdAt  Timestamp instant of creation
  * @param modelId    ID of model the stat is on
  * @param address    Client address
  * @param cookie     Browser cookie
  * @param userId     User ID
  */
case class VersionDownload(override val id: Option[Int] = None,
                           override val createdAt: Option[Timestamp] = None,
                           override val modelId: Int,
                           override val address: InetString,
                           override val cookie: String,
                           @(Bind @field) private var userId: Option[Int] = None)
                           extends StatEntry[Version](id, createdAt, modelId, address, cookie, userId) {

  BindingsGenerator.generateFor(this)

  override def subject: Version = Version.withId(this.modelId).get

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = theTime)

}

object VersionDownload {

  /**
    * Creates a new VersionDownload to be (or not be) recorded from an incoming
    * request.
    *
    * @param version  Version downloaded
    * @param request  Request to bind
    * @return         New VersionDownload
    */
  def bindFromRequest(version: Version)(implicit request: RequestHeader): VersionDownload = {
    val cookie = Statistics.getStatCookie
    val userId = User.current(request.session).flatMap(_.id)
    VersionDownload(
      modelId = version.id.get,
      address = InetString(request.remoteAddress),
      cookie = cookie,
      userId = userId
    )
  }

}