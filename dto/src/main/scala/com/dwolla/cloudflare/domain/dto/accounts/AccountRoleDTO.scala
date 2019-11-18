package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class AccountRoleDTO (
  id: String,
  name: String,
  description: String,
  permissions: Map[String, AccountRolePermissionsDTO]
)

object AccountRoleDTO {
  implicit val accountRoleDTOCodec: Codec[AccountRoleDTO] = deriveCodec
}

case class AccountRolePermissionsDTO (
  read: Boolean,
  edit: Boolean
)

object AccountRolePermissionsDTO {
  implicit val accountRolePermissionsDTOCodec: Codec[AccountRolePermissionsDTO] = deriveCodec
}
