package com.dwolla.cloudflare.domain.dto.accounts

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}

case class AccountRoleDTO (
  id: String,
  name: String,
  description: String,
  permissions: Map[String, AccountRolePermissionsDTO]
)

object AccountRoleDTO {
  implicit val accountRoleDTOEncoder: Encoder[AccountRoleDTO] = deriveEncoder
  implicit val accountRoleDTODecoder: Decoder[AccountRoleDTO] = deriveDecoder
}

case class AccountRolePermissionsDTO (
  read: Boolean,
  edit: Boolean
)

object AccountRolePermissionsDTO {
  implicit val accountRolePermissionsDTOEncoder: Encoder[AccountRolePermissionsDTO] = deriveEncoder
  implicit val accountRolePermissionsDTODecoder: Decoder[AccountRolePermissionsDTO] = deriveDecoder
}
