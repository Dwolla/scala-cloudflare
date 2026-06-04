package com.dwolla.cloudflare
package domain
package model
package accounts

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class AccountRole (
  id: String,
  name: String,
  description: String,
  permissions: Map[String, AccountRolePermissions]
)

object AccountRole {
  implicit val accountRoleCodec: Codec[AccountRole] = deriveCodec
}

case class AccountRolePermissions (
  read: Boolean,
  edit: Boolean
)

object AccountRolePermissions {
  implicit val accountRolePermissionsCodec: Codec[AccountRolePermissions] = deriveCodec
}
