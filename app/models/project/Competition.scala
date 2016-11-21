package models.project

import java.sql.Timestamp

import db.Named
import db.impl.CompetitionTable
import db.impl.model.OreModel
import db.impl.model.common.Describable

case class Competition(override val id: Option[Int],
                       override val createdAt: Option[Timestamp],
                       override val name: String,
                       private var _description: Option[String],
                       private var _startDate: Timestamp,
                       private var _endDate: Timestamp,
                       private var _isVotingEnabled: Boolean = true,
                       private var _isStaffVotingOnly: Boolean = false,
                       private var _shouldShowVoteCount: Boolean = true,
                       private var _isSpongeOnly: Boolean = false,
                       private var _isSourceRequired: Boolean = false,
                       private var _defaultVotes: Int = 1,
                       private var _staffVotes: Int = 1,
                       private var _allowedEntries: Int = 1,
                       private var _maxEntryTotal: Int = -1)
                       extends OreModel(id, createdAt) with Named with Describable {

  override type M = Competition
  override type T = CompetitionTable

  override def description: Option[String] = this._description

  def description_=(description: String) = {
    this._description = Option(description)
  }

  def startDate: Timestamp = this._startDate

  def startDate_=(startDate: Timestamp) = {
    this._startDate = startDate
  }

  def endDate: Timestamp = this._endDate

  def endDate_=(endDate: Timestamp) = {
    this._endDate = endDate
  }

  def isVotingEnabled: Boolean = this._isVotingEnabled

  def setVotingEnabled(votingEnabled: Boolean) = {
    this._isVotingEnabled = votingEnabled
  }

  def isStaffVotingOnly: Boolean = this._isStaffVotingOnly

  def setStaffVotingOnly(staffVotingOnly: Boolean) = {
    this._isStaffVotingOnly = staffVotingOnly
  }

  def shouldShowVoteCount: Boolean = this._shouldShowVoteCount

  def setShouldShowVoteCount(shouldShowVoteCount: Boolean) = {
    this._shouldShowVoteCount = shouldShowVoteCount
  }

  def isSpongeOnly: Boolean = this._isSpongeOnly

  def setSpongeOnly(spongeOnly: Boolean) = {
    this._isSpongeOnly = spongeOnly
  }

  def isSourceRequired: Boolean = this._isSourceRequired

  def setSourceRequired(sourceRequired: Boolean) = {
    this._isSourceRequired = sourceRequired
  }

  def defaultVotes: Int = this._defaultVotes

  def defaultVotes_=(defaultVotes: Int) = {
    this._defaultVotes = defaultVotes
  }

  def staffVotes: Int = this._staffVotes

  def staffVotes_=(staffVotes: Int) = {
    this._staffVotes = staffVotes
  }

  def allowedEntries: Int = this._allowedEntries

  def allowedEntries_=(allowedEntries: Int) = {
    this._allowedEntries = allowedEntries
  }

  def maxEntryTotal: Int = this._maxEntryTotal

  def maxEntryTotal_=(maxEntryTotal: Int) = {
    this._maxEntryTotal = maxEntryTotal
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
