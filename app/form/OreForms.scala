package form

import javax.inject.Inject

import form.organization.{OrganizationAvatarUpdate, OrganizationMembersUpdate, OrganizationRoleSetBuilder}
import form.project._
import models.project.Channel
import models.project.Page._
import models.user.role.ProjectRole
import ore.OreConfig
import ore.project.factory.ProjectFactory
import play.api.data.Form
import play.api.data.Forms._

/**
  * Collection of forms used in this application.
  */
//noinspection ConvertibleToMethodValue
class OreForms @Inject()(implicit config: OreConfig, factory: ProjectFactory) {

  /**
    * Submits a member to be removed from a Project.
    */
  lazy val ProjectMemberRemove = Form(single("username" -> nonEmptyText))

  /**
    * Submits changes to a [[models.project.Project]]'s
    * [[ProjectRole]]s.
    */
  lazy val ProjectMemberRoles = Form(mapping(
    "users" -> list(number),
    "roles" -> list(text)
  )(ProjectRoleSetBuilder.apply)(ProjectRoleSetBuilder.unapply))

  /**
    * Submits a flag on a project for further review.
    */
  lazy val ProjectFlag = Form(single("flag-reason" -> number))

  /**
    * Submits settings changes for a Project.
    */
  lazy val ProjectSave = Form(mapping(
    "category" -> text,
    "issues" -> text,
    "source" -> text,
    "license-name" -> text,
    "license-url" -> text,
    "description" -> text,
    "users" -> list(number),
    "roles" -> list(text),
    "userUps" -> list(text),
    "roleUps" -> list(text),
    "update-icon" -> boolean,
    "owner" -> optional(number)
  )(ProjectSettingsForm.apply)(ProjectSettingsForm.unapply))

  /**
    * Submits a name change for a project.
    */
  lazy val ProjectRename = Form(single("name" -> text))

  /**
    * Submits a post reply for a project discussion.
    */
  lazy val ProjectReply = Form(single("content" -> text(minLength = MinLength, maxLength = MaxLength)))

  /**
    * Submits a list of organization members to be invited.
    */
  lazy val OrganizationCreate = Form(mapping(
    "name" -> nonEmptyText,
    "users" -> list(number),
    "roles" -> list(text)
  )(OrganizationRoleSetBuilder.apply)(OrganizationRoleSetBuilder.unapply))

  /**
    * Submits an avatar update for an [[models.user.Organization]].
    */
  lazy val OrganizationUpdateAvatar = Form(mapping(
    "avatar-method" -> nonEmptyText,
    "avatar-url" -> optional(nonEmptyText)
  )(OrganizationAvatarUpdate.apply)(OrganizationAvatarUpdate.unapply))

  /**
    * Submits an organization member for removal.
    */
  lazy val OrganizationMemberRemove = Form(single("username" -> nonEmptyText))

  /**
    * Submits a list of members to be added or updated.
    */
  lazy val OrganizationUpdateMembers = Form(mapping(
    "users" -> list(number),
    "roles" -> list(text),
    "userUps" -> list(text),
    "roleUps" -> list(text)
  )(OrganizationMembersUpdate.apply)(OrganizationMembersUpdate.unapply))

  /**
    * Submits a new Channel for a Project.
    */
  lazy val ChannelEdit = Form(mapping(
    "channel-input" -> text.verifying(
      "Invalid channel name.", config.isValidChannelName(_)
    ),

    "channel-color-input" -> text.verifying(
      "Invalid channel color.", c => Channel.Colors.exists(_.hex.equalsIgnoreCase(c))
    )
  )(ChannelData.apply)(ChannelData.unapply))

  /**
    * Submits changes on a documentation page.
    */
  lazy val PageEdit = Form(single(
    "content" -> text(
      minLength = MinLength,
      maxLength = MaxLength
    )))

  /**
    * Submits a tagline change for a User.
    */
  lazy val UserTagline = Form(single("tagline" -> text))

  /**
    * Submits a PGP public key update.
    */
  lazy val UserPgpPubKey = Form(mapping(
    "pgp-pub-key" -> text
  )(PGPPublicKeySubmission.apply)(PGPPublicKeySubmission.unapply) verifying("error.invalidKey", _.validateKey()))

  /**
    * Submits a new Version.
    */
  lazy val VersionCreate = Form(mapping(
    "recommended" -> boolean,
    "channel-input" -> text.verifying(
      "Invalid channel name.", config.isValidChannelName(_)),
    "channel-color-input" -> text.verifying(
      "Invalid channel color.", c => Channel.Colors.exists(_.hex.equalsIgnoreCase(c))),
    "content" -> optional(text)
  )(VersionData.apply)(VersionData.unapply))

  lazy val CompetitionCreate = Form(mapping(
    "name" -> nonEmptyText(0, this.config.ore.getInt("competitions.name.maxLen").get),
    "description" -> optional(nonEmptyText),
    "start-date" -> date,
    "end-date" -> date,
    "enable-voting" -> default(boolean, true),
    "staff-only" -> default(boolean, false),
    "show-vote-count" -> default(boolean, true),
    "sponge-only" -> default(boolean, false),
    "source-required" -> default(boolean, false),
    "default-votes" -> default(number(0), 1),
    "staff-votes" -> default(number(0), 1),
    "default-entries" -> default(number(1), 1),
    "max-entries-total" -> default(number(-1), -1)
  )(CompetitionData.apply)(CompetitionData.unapply))

  /**
    * Submits a change to a Version's description.
    */
  lazy val VersionDescription = Form(single("content" -> text))

}
