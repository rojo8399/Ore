package ore

import javax.inject.Inject

import models.project.Channel
import ore.Colors._
import org.spongepowered.plugin.meta.version.ComparableVersion
import play.api.{Configuration, Logger}
import util.StringUtils._

/**
  * A helper class for the Ore configuration.
  *
  * @param config Base configuration file
  */
final class OreConfig @Inject()(config: Configuration) {

  // Sub-configs
  lazy val root = this.config
  lazy val app = this.config.getConfig("application").get
  lazy val play = this.config.getConfig("play").get
  lazy val ore = this.config.getConfig("ore").get
  lazy val channels = this.ore.getConfig("channels").get
  lazy val pages = this.ore.getConfig("pages").get
  lazy val projects = this.ore.getConfig("projects").get
  lazy val users = this.ore.getConfig("users").get
  lazy val orgs = this.ore.getConfig("orgs").get
  lazy val competitions = this.ore.getConfig("competitions").get
  lazy val forums = this.root.getConfig("discourse").get
  lazy val sponge = this.root.getConfig("sponge").get
  lazy val security = this.root.getConfig("security").get

  /**
    * The default color used for Channels.
    */
  lazy val defaultChannelColor: Color = Channel.Colors(this.channels.getInt("color-default").get)

  /**
    * The default name used for Channels.
    */
  lazy val defaultChannelName: String = this.channels.getString("name-default").get

  /**
    * Returns true if the specified name is a valid Project name.
    *
    * @param name   Name to check
    * @return       True if valid name
    */
  def isValidProjectName(name: String): Boolean = {
    val sanitized = compact(name)
    sanitized.length >= 1 && sanitized.length <= this.projects.getInt("max-name-len").get
  }

  /**
    * Returns true if the specified string is a valid channel name.
    *
    * @param name   Name to check
    * @return       True if valid channel name
    */
  def isValidChannelName(name: String): Boolean = {
    val c = this.channels
    name.length >= 1 && name.length <= c.getInt("max-name-len").get && name.matches(c.getString("name-regex").get)
  }

  /**
    * Attempts to determine a Channel name from the specified version string.
    * This is attained using a ComparableVersion and finding the first
    * StringItem within the parsed version. (e.g. 1.0.0-alpha) would return
    * "alpha".
    *
    * @param version  Version string to parse
    * @return         Suggested channel name
    */
  def getSuggestedNameForVersion(version: String): String
  = Option(new ComparableVersion(version).getFirstString).getOrElse(this.defaultChannelName)

  lazy val debugLevel = this.ore.getInt("debug-level").get

  /** Returns true if the application is running in debug mode. */
  def isDebug: Boolean = this.ore.getBoolean("debug").get

  /** Sends a debug message if in debug mode */
  def debug(msg: Any, level: Int = 1) = {
    if (isDebug && (level == this.debugLevel || level == -1))
      Logger.debug(msg.toString)
  }

  /** Asserts that the application is in debug mode. */
  def checkDebug() = {
    if(!isDebug)
      throw new UnsupportedOperationException("this function is supported in debug mode only")
  }

}
