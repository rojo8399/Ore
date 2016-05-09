package models.user

import java.sql.Timestamp
import java.util.Date
import javax.inject.Inject

import ore.permission.role.RoleTypes
import util.OreConfig

/**
  * Represents a "fake" User object for bypassing the standard authentication
  * method in a development environment.
  */
class FakeUser @Inject()(config: OreConfig) {

  private lazy val conf = config.app

  /**
    * True if FakeUser should be used.
    */
  lazy val isEnabled: Boolean = conf.getBoolean("fakeUser.enabled").get

  lazy private val user = new User(
    id           =   conf.getInt("fakeUser.id"),
    _name        =   conf.getString("fakeUser.name"),
    _username    =   conf.getString("fakeUser.username").get,
    _email       =   conf.getString("fakeUser.email"),
    _joinDate    =   Some(new Timestamp(new Date().getTime)),
    _globalRoles =   List(RoleTypes.Admin)
  )

}

object FakeUser {
  implicit def unwrap(fake: FakeUser): User = fake.user
}
