package models.project

import java.sql.Timestamp
import java.util.Date

import db.Named
import db.impl.CompetitionTable
import db.impl.model.OreModel
import db.impl.model.common.Describable
import db.impl.table.ModelKeys._
import form.project.competition.{CompetitionCreateForm, CompetitionSaveForm}
import models.user.User
import ore.user.UserOwned
import util.StringUtils.{localDateTime2timestamp, nullIfEmpty}

import scala.concurrent.duration._

case class Competition(override val id: Option[Int] = None,
                       override val createdAt: Option[Timestamp] = None,
                       override val userId: Int,
                       override val name: String,
                       private var _description: Option[String] = None,
                       private var _startDate: Timestamp,
                       private var _endDate: Timestamp,
                       private var _timeZone: String,
                       private var _isVotingEnabled: Boolean = true,
                       private var _isStaffVotingOnly: Boolean = false,
                       private var _shouldShowVoteCount: Boolean = true,
                       isSpongeOnly: Boolean,
                       private var _isSourceRequired: Boolean = false,
                       private var _defaultVotes: Int = 1,
                       private var _staffVotes: Int = 1,
                       private var _allowedEntries: Int = 1,
                       private var _maxEntryTotal: Int = -1)
                       extends OreModel(id, createdAt) with Named with Describable with UserOwned {

  override type M = Competition
  override type T = CompetitionTable

  def this(user: User, formData: CompetitionCreateForm) = this(
    userId = user.id.get,
    name = formData.name.trim,
    _description = Option(formData.description.map(s => nullIfEmpty(s.trim)).orNull),
    _startDate = localDateTime2timestamp(formData.startDate, formData.timeZoneId),
    _endDate = localDateTime2timestamp(formData.endDate, formData.timeZoneId),
    _timeZone = formData.timeZoneId,
    _isVotingEnabled = formData.isVotingEnabled,
    _isStaffVotingOnly = formData.isStaffVotingOnly,
    _shouldShowVoteCount = formData.shouldShowVoteCount,
    isSpongeOnly = formData.isSpongeOnly,
    _isSourceRequired = formData.isSourceRequired,
    _defaultVotes = formData.defaultVotes,
    _staffVotes = formData.staffVotes,
    _allowedEntries = formData.allowedEntries,
    _maxEntryTotal = formData.maxEntryTotal
  )

  def save(formData: CompetitionSaveForm) = {
    this.startDate = localDateTime2timestamp(formData.startDate, formData.timeZoneId)
    this.endDate = localDateTime2timestamp(formData.endDate, formData.timeZoneId)
    this.setVotingEnabled(formData.isVotingEnabled)
    this.setStaffVotingOnly(formData.isStaffVotingOnly)
    this.setShouldShowVoteCount(formData.shouldShowVoteCount)
    this.setSourceRequired(formData.isSourceRequired)
    this.defaultVotes = formData.defaultVotes
    this.staffVotes = formData.staffVotes
    this.allowedEntries = formData.allowedEntries
    this.maxEntryTotal = formData.maxEntryTotal
  }

  override def description: Option[String] = this._description

  def description_=(description: String) = {
    this._description = Option(description)
    if (isDefined) update(Description)
  }

  def startDate: Timestamp = this._startDate

  def startDate_=(startDate: Timestamp) = {
    this._startDate = startDate
    if (isDefined) update(StartDate)
  }

  def endDate: Timestamp = this._endDate

  def endDate_=(endDate: Timestamp) = {
    this._endDate = endDate
    if (isDefined) update(EndDate)
  }

  def timeZone: String = this._timeZone

  def timeZone_=(timeZone: String) = {
    this._timeZone = timeZone
    if (isDefined) update(TimeZone)
  }

  def timeRemaining: Duration = (this.endDate.getTime - new Date().getTime).millis

  def isVotingEnabled: Boolean = this._isVotingEnabled

  def setVotingEnabled(votingEnabled: Boolean) = {
    this._isVotingEnabled = votingEnabled
    if (isDefined) update(IsVotingEnabled)
  }

  def isStaffVotingOnly: Boolean = this._isStaffVotingOnly

  def setStaffVotingOnly(staffVotingOnly: Boolean) = {
    this._isStaffVotingOnly = staffVotingOnly
    if (isDefined) update(IsStaffVotingOnly)
  }

  def shouldShowVoteCount: Boolean = this._shouldShowVoteCount

  def setShouldShowVoteCount(shouldShowVoteCount: Boolean) = {
    this._shouldShowVoteCount = shouldShowVoteCount
    if (isDefined) update(ShouldShowVoteCount)
  }

  def isSourceRequired: Boolean = this._isSourceRequired

  def setSourceRequired(sourceRequired: Boolean) = {
    this._isSourceRequired = sourceRequired
    if (isDefined) update(IsSourceRequired)
  }

  def defaultVotes: Int = this._defaultVotes

  def defaultVotes_=(defaultVotes: Int) = {
    this._defaultVotes = defaultVotes
    if (isDefined) update(DefaultVotes)
  }

  def staffVotes: Int = this._staffVotes

  def staffVotes_=(staffVotes: Int) = {
    this._staffVotes = staffVotes
    if (isDefined) update(StaffVotes)
  }

  def allowedEntries: Int = this._allowedEntries

  def allowedEntries_=(allowedEntries: Int) = {
    this._allowedEntries = allowedEntries
    if (isDefined) update(AllowedEntries)
  }

  def maxEntryTotal: Int = this._maxEntryTotal

  def maxEntryTotal_=(maxEntryTotal: Int) = {
    this._maxEntryTotal = maxEntryTotal
    if (isDefined) update(MaxEntryTotal)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
