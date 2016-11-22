package db.impl.table

import db.Named
import db.impl.OrePostgresDriver.api._
import db.impl.model.common.{Describable, Downloadable, Hideable}
import db.table.key._
import models.project._
import models.statistic.StatEntry
import models.user.role.RoleModel
import models.user.{Notification, User}
import ore.Colors.Color
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category
import ore.user.Prompts.Prompt

/**
  * Collection of String keys used for table bindings within Models.
  */
object ModelKeys {

  // Shared
  val Name                  =   new StringKey[Named](_.name, _.name)
  val Downloads             =   new IntKey[Downloadable](_.downloads, _.downloadCount)
  val Description           =   new StringKey[Describable](_.description, _.description.orNull)
  val IsVisible             =   new BooleanKey[Hideable](_.isVisible, _.isVisible)

  // Project
  val OwnerId               =   new IntKey[Project](_.userId, _.ownerId)
  val OwnerName             =   new StringKey[Project](_.ownerName, _.ownerName)
  val Slug                  =   new StringKey[Project](_.slug, _.slug)
  val Category              =   new MappedTypeKey[Project, Category](_.category, _.category)
  val IsSpongePlugin        =   new BooleanKey[Project](_.isSpongePlugin, _.isSpongePlugin)
  val IsForgeMod            =   new BooleanKey[Project](_.isForgeMod, _.isForgeMod)
  val Stars                 =   new IntKey[Project](_.stars, _.starCount)
  val Views                 =   new IntKey[Project](_.views, _.viewCount)
  val TopicId               =   new IntKey[Project](_.topicId, _.topicId)
  val PostId                =   new IntKey[Project](_.postId, _.postId)
  val IsTopicDirty          =   new BooleanKey[Project](_.isTopicDirty, _.isTopicDirty)
  val RecommendedVersionId  =   new IntKey[Project](
                                  _.recommendedVersionId, _.recommendedVersion.id.getOrElse(-1))
  val LastUpdated           =   new TimestampKey[Project](_.lastUpdated, _.lastUpdated)

  // ProjectSettings
  val Issues                =   new StringKey[ProjectSettings](_.issues, _.issues.orNull)
  val Source                =   new StringKey[ProjectSettings](_.source, _.source.orNull)
  val LicenseName           =   new StringKey[ProjectSettings](_.licenseName, _.licenseName.orNull)
  val LicenseUrl            =   new StringKey[ProjectSettings](_.licenseUrl, _.licenseUrl.orNull)

  // User
  val FullName              =   new StringKey[User](_.fullName, _.fullName.orNull)
  val Email                 =   new StringKey[User](_.email, _.email.orNull)
  val PGPPubKey             =   new StringKey[User](_.pgpPubKey, _.pgpPubKey.orNull)
  val LastPGPPubKeyUpdate   =   new TimestampKey[User](_.lastPgpPubKeyUpdate, _.lastPgpPubKeyUpdate.orNull)
  val IsLocked              =   new BooleanKey[User](_.isLocked, _.isLocked)
  val Tagline               =   new StringKey[User](_.tagline, _.tagline.orNull)
  val GlobalRoles           =   new Key[User, List[RoleType]](_.globalRoles, _.globalRoles.toList)
  val JoinDate              =   new TimestampKey[User](_.joinDate, _.joinDate.orNull)
  val AvatarUrl             =   new StringKey[User](_.avatarUrl, _.avatarUrl.orNull)
  val ReadPrompts           =   new Key[User, List[Prompt]](_.readPrompts, _.readPrompts.toList)

  // Competition
  val StartDate             =   new TimestampKey[Competition](_.startDate, _.startDate)
  val EndDate               =   new TimestampKey[Competition](_.endDate, _.endDate)
  val TimeZone              =   new StringKey[Competition](_.timeZone, _.timeZone)
  val IsVotingEnabled       =   new BooleanKey[Competition](_.isVotingEnabled, _.isVotingEnabled)
  val IsStaffVotingOnly     =   new BooleanKey[Competition](_.isStaffVotingOnly, _.isStaffVotingOnly)
  val ShouldShowVoteCount   =   new BooleanKey[Competition](_.shouldShowVoteCount, _.shouldShowVoteCount)
  val IsSourceRequired      =   new BooleanKey[Competition](_.isSourceRequired, _.isSourceRequired)
  val DefaultVotes          =   new IntKey[Competition](_.defaultVotes, _.defaultVotes)
  val StaffVotes            =   new IntKey[Competition](_.staffVotes, _.staffVotes)
  val AllowedEntries        =   new IntKey[Competition](_.allowedEntries, _.allowedEntries)
  val MaxEntryTotal         =   new IntKey[Competition](_.maxEntryTotal, _.maxEntryTotal)

  // Version
  val IsReviewed            =   new BooleanKey[Version](_.isReviewed, _.isReviewed)
  val ChannelId             =   new IntKey[Version](_.channelId, _.channelId)

  // Channel
  val Color                 =   new MappedTypeKey[Channel, Color](_.color, _.color)

  // Page
  val Contents              =   new StringKey[Page](_.contents, _.contents)

  // RoleModel
  val RoleType              =   new MappedTypeKey[RoleModel, RoleType](_.roleType, _.roleType)
  val IsAccepted            =   new BooleanKey[RoleModel](_.isAccepted, _.isAccepted)

  // Flag
  val IsResolved            =   new BooleanKey[Flag](_.isResolved, _.isResolved)

  // StatEntry
  val UserId                =   new IntKey[StatEntry[_]](_.userId, _.user.flatMap(_.id).getOrElse(-1))

  // Notification
  val Read                  =   new BooleanKey[Notification](_.read, _.isRead)

}
